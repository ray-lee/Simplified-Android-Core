package org.nypl.simplified.splash

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.io7m.jnull.NullCheck
import org.json.JSONObject
import org.nypl.drm.core.Assertions
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.opds.core.OPDSJSONParser
import java.io.File
import java.util.concurrent.Callable
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import java.net.URI

/**
 * DatabaseMigrationTask is a utility class to check the whether or not a user's
 * data has been migrated to the new LFA-style database destination. This utility
 * copies that data to the new destination, and then delete the old data.
 * See: https://jira.nypl.org/browse/SIMPLY-2068
 *
 * @param context context for SharePreferences
 * @param account for the user's database
 */

class DataMigrationTask(val context: Context, val account: AccountType) : Callable<Unit> {

    private var sharedPreferences: SharedPreferences
    private val META_FILENAME = "meta.json"
    private val EPUB_FILENAME = "book.epub"
    private val AUDIOBOOK_FILENAME = "audiobook-manifest.json"
    private val AUDIOBOOK_MANIFEST_URI_FILENAME = "audiobook-manifest-uri.txt"

    private lateinit var databaseEntry: org.nypl.simplified.books.book_database.api.BookDatabaseEntryType

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "DatabaseMigrationTask"

        /**
         * The default shared preference name
         */
        const val PREFERENCE_NAME = "DATA"

        /**
         * SharePreferences keys
         */
        private const val KEY_DATA_MIGRATION = "DataMigrationCompleted"
    }

    override fun call() {
        migrateData()
    }

    /**
     * Migrate each book
     *
     * @param directory
     */
    private fun migrateBooks(directory: File) {
        val database = account.bookDatabase()
        Log.d(TAG, "Now migrating books...")
        val startTime = System.currentTimeMillis()
        val bookFiles: Collection<Triple<File, File?, URI?>> = listFileTree(directory)
        for (book in bookFiles) {
            val bookJson = JSONObject(book.first.inputStream().bufferedReader().use { it.readText() })
            Log.d(TAG, bookJson.toString())
            val parser = OPDSJSONParser.newParser()
            val bookEntry = parser.parseAcquisitionFeedEntryFromStream(book.first.inputStream())
            //TODO: Here the book ID is a url? This doesn't look right, Book IDs must be non-empty, alphanumeric lowercase ([a-z0-9]+)
            val bookID = BookID.create(bookEntry.id)

            databaseEntry = database.createOrUpdate(bookID, bookEntry)
            val format = databaseEntry.findPreferredFormatHandle()
            return when (format) {
                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB -> {
                    if (book.second != null) {
                        format.copyInBook(book.second!!)
                    } else {
                        //TODO: Throw exception here or silently fail?
                    }
                }
                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook -> {
                    if (book.second != null && book.third != null) {
                        format.copyInManifestAndURI(book.second!!, book.third!!)
                    } else {
                        //TODO: Throw exception here or silently fail?
                    }
                }
                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF -> {
                    // Do nothing for now
                }
                null -> { }
            }
        }
        cleanUp(directory)
        dataMigrated = true
        val endTime = System.currentTimeMillis()
        val seconds = (endTime - startTime) / 1000
        Log.d(TAG, "Done migrating books, took $seconds seconds")
    }

    /**
     * Get list of pairs of meta and bookfiles in the app directory
     *
     * @param dir
     * @return List of meta/bookfile triples
     */
    private fun listFileTree(dir: File?): Collection<Triple<File, File?, URI?>> {
        val fileTree = HashSet<Triple<File, File?, URI?>>()
        // Check to see if the directory even exists
        if (dir?.listFiles() == null) {
            return fileTree
        }
        // Loop through all of the files in the directory
        for (entry in dir.listFiles()!!) {
            // Find all of the meta.json files
            if (entry.isFile && entry.name == META_FILENAME) {
                // Back up to the director that contains meta.json
                val bookDirectory = entry.parentFile
                // TODO: Perhaps use DirectoryUtilities or FileUtilities to make this cleaner
                // Find the EPUB and audibook files to pair with meta.json
                for (item in bookDirectory.listFiles()) {
                    var audiobookUri: URI? = null
                    var bookFile: File? = null

                    // Check if the file is of type audiobook or EPUB
                    if (item.isFile && item.name == EPUB_FILENAME || item.name == AUDIOBOOK_FILENAME) {
                        bookFile = item
                    }
                    if (item.isFile && item.name == AUDIOBOOK_MANIFEST_URI_FILENAME) {
                        audiobookUri = item.toURI()
                    }
                    fileTree.add(Triple(entry, bookFile, audiobookUri))
                }
            } else {
                fileTree.addAll(listFileTree(entry))
            }
        }
        return fileTree
    }

    /**
     * Getter/Setter for user's data migration status
     */
    var dataMigrated: Boolean? = null
        get() {
            if (field == null) {
                field = sharedPreferences.getBoolean(KEY_DATA_MIGRATION, false)
            }
            return field
        }
        set(value) {
            field = value
            sharedPreferences.edit().putBoolean(KEY_DATA_MIGRATION, value!!).apply()
        }

    /**
     * Copies files from old location and moves them to the new location
     */
    private fun migrateData() {
        val dataDirectory = getDiskDataDir()
        migrateBooks(dataDirectory)
    }

    /**
     * Deletes database files from the old location
     */
    private fun cleanUp(file: File) {
        file.delete()
    }

    /**
     * Gets the directory where Profile/Books are being stored
     *
     * @return
     */
    private fun getDiskDataDir(): File {
        // If external storage is mounted and is on a device that doesn't allow
        // the storage to be removed, use the external storage for data.
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            Log.d(TAG, "trying external storage")
            if (!Environment.isExternalStorageRemovable()) {
                val file: File = context.getExternalFilesDir(null)
                Log.d(TAG, "external storage is not removable, using it ({})")
                Assertions.checkPrecondition(file.isDirectory, "Data directory {} is a directory")
                return NullCheck.notNull(file)
            }
        }

        // Otherwise, use internal storage.
        val file: File = context.filesDir
        Log.d(TAG, "no non-removable external storage, using internal storage ({})")
        Assertions.checkPrecondition(file.isDirectory, "Data directory {} is a directory")
        return NullCheck.notNull(file)
    }
}
