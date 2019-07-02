package org.nypl.simplified.books.controller

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.io7m.jfunctional.FunctionType
import com.io7m.jnull.NullCheck
import org.nypl.drm.core.Assertions
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Callable

class ProfileMigrationTask(
        val context: Context,
        val accountProviders: FunctionType<com.io7m.jfunctional.Unit, AccountProviderCollectionType>
        val profile: ProfileType)
    : Callable<Unit> {

    private val BOOK_JSON_FILENAME = "meta.json"
    private val EPUB_FILENAME = "book.epub"
    private val AUDIOBOOK_FILENAME = "audiobook-manifest.json"
    private val AUDIOBOOK_MANIFEST_URI_FILENAME = "audiobook-manifest-uri.txt"
    private var sharedPreferences: SharedPreferences
    private val logger = LoggerFactory.getLogger(ProfileMigrationTask::class.java)

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "ProfileabaseMigrationTask"

        /**
         * The default shared preference name
         */
        const val PREFERENCE_NAME = "PROFILE"

        /**
         * SharePreferences keys
         */
        private const val KEY_PROFILE_MIGRATION = "ProfileMigrationCompleted"
    }

    /**
     * Getter/Setter for profile migration status
     */
    var profilesMigrated: Boolean? = null
        get() {
            if (field == null) {
                field = sharedPreferences.getBoolean(KEY_PROFILE_MIGRATION, false)
            }
            return field
        }
        set(value) {
            field = value
            sharedPreferences.edit().putBoolean(KEY_PROFILE_MIGRATION, value!!).apply()
        }

    override fun call() {
        if (profilesMigrated == false) {
            logger.debug("Now migrating profiles..")
            val startTime = System.currentTimeMillis()

            val oldBaseDirectory = getDiskDataDir()
            val oldAccountDirectories = oldBaseDirectory.list()

            for (oldAccount in oldAccountDirectories) {
                val accountId = oldAccount
                val booksDirectory = File(oldAccount, "books")
                val accountProvider = accountProviders.call(com.io7m.jfunctional.Unit.unit())
                for (provider in accountProvider.providers()) {
                    if (provider.value.idNumeric() == accountId) {
                        val account = profile.createAccount(provider.value)
                        val booksDatabase = account.bookDatabase()

                        for (book in booksDirectory.list()) {
                            val booksDatabase = account.bookDatabase()
                            val parser = OPDSJSONParser.newParser()
                            val bookEntry = parser.parseAcquisitionFeedEntryFromStream(File(book, BOOK_JSON_FILENAME).inputStream())
                            val bookID = BookIDs.newFromText(bookEntry.id)
                            val entry = booksDatabase.createOrUpdate(bookID, bookEntry)

                            return when (val handle = entry.findPreferredFormatHandle()) {
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB -> {
                                    handle.copyInBook(File(book, EPUB_FILENAME))
                                }
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF -> {
                                }
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook -> {
                                    handle.copyInManifestFile(File(book, AUDIOBOOK_FILENAME))
                                }
                                null -> {
                                }
                            }
                        }
                    }
                }
            }

            cleanUp(oldBaseDirectory)
            profilesMigrated = true
            val endTime = System.currentTimeMillis()
            val seconds = (endTime - startTime) / 1000
            logger.debug("Done migrating books, took $seconds seconds")
        } else {
            return
        }
    }

    /**
     * Deletes database files from the old location
     */
    private fun cleanUp(directory: File) {
        DirectoryUtilities.directoryDelete(directory)
    }

    /**
     * Gets the directory where Profiles/Books are being stored
     *
     * @return
     */
    private fun getDiskDataDir(): File {
        // If external storage is mounted and is on a device that doesn't allow
        // the storage to be removed, use the external storage for data.
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            logger.debug("trying external storage")
            if (!Environment.isExternalStorageRemovable()) {
                val file: File = context.getExternalFilesDir(null)
                logger.debug("external storage is not removable, using it ({})")
                Assertions.checkPrecondition(file.isDirectory, "Data directory {} is a directory")
                return NullCheck.notNull(file)
            }
        }

        // Otherwise, use internal storage.
        val file: File = context.filesDir
        logger.debug("no non-removable external storage, using internal storage ({})")
        Assertions.checkPrecondition(file.isDirectory, "Data directory {} is a directory")
        return NullCheck.notNull(file)
    }
}
