package org.nypl.simplified.books.controller

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import com.io7m.jfunctional.FunctionType
import com.io7m.jnull.NullCheck
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.Assertions
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Callable

class ProfileMigrationTask(
        val context: Context,
        val accountProviders: FunctionType<com.io7m.jfunctional.Unit, AccountProviderCollectionType>,
        val profile: ProfileType)
    : Callable<Unit> {

    private val BOOK_JSON_FILENAME = "meta.json"
    private val EPUB_FILENAME = "book.epub"
    private val ADOBE_RIGHTS_FILENAME = "rights_adobe.xml"
    private val ADOBE_META_FILENAME = "meta_adobe.json"
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
                val accountProviders = accountProviders.call(com.io7m.jfunctional.Unit.unit())
                val accountsByProvider = profile.accountsDatabase().accountsByProvider()

                for (accountProvider in accountProviders.providers().values) {
                    if (accountProvider.idNumeric() == accountId.toInt()) {

                        /*
                         * Check to see if the user already has an account that uses a matching
                         * provider. If there is one, reuse it rather than creating a new one.
                         */

                        val account =
                          if (accountsByProvider.containsKey(accountProvider.id())) {
                              accountsByProvider[accountProvider.id()]!!
                          } else {
                              profile.createAccount(accountProvider)
                          }

                        val booksDatabase = account.bookDatabase()
                        val parser = OPDSJSONParser.newParser()

                        for (book in booksDirectory.list()) {

                            /*
                             * Parse the acquisition feed entry.
                             */

                            val fileMeta = File(book, BOOK_JSON_FILENAME)
                            val bookEntry =
                              fileMeta.inputStream().use { stream ->
                                parser.parseAcquisitionFeedEntryFromStream(stream)
                            }

                            val bookID = BookIDs.newFromText(bookEntry.id)
                            val entry = booksDatabase.createOrUpdate(bookID, bookEntry)

                            when (val handle = entry.findPreferredFormatHandle()) {
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB -> {
                                    handle.copyInBook(File(book, EPUB_FILENAME))
                                    val fileAdobeRights = File(book, ADOBE_RIGHTS_FILENAME)
                                    val fileAdobeMeta = File(book, ADOBE_META_FILENAME)
                                    handle.setAdobeRightsInformation(
                                      parseAdobeLoanIfPresent(fileAdobeRights, fileAdobeMeta))
                                }
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF -> {

                                }
                                is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook -> {
                                    val fileManifest = File(book, AUDIOBOOK_FILENAME)
                                    val fileURI = File(book, AUDIOBOOK_MANIFEST_URI_FILENAME)
                                    val manifestURI = URI(fileURI.readText(Charsets.UTF_8))
                                    handle.copyInManifestAndURI(fileManifest, manifestURI)
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
     * Parse Adobe loan information if it exists, otherwise ignore it.
     */

    private fun parseAdobeLoanIfPresent(fileAdobeRights: File, fileAdobeMeta: File): AdobeAdeptLoan? {
        return if (fileAdobeRights.isFile) {
            val serialized = fileAdobeRights.readBytes()
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(fileAdobeMeta)
            val objectNode = JSONParserUtilities.checkObject(null, jsonNode)
            val loanID = AdobeLoanID(JSONParserUtilities.getString(objectNode, "loan-id"))
            val returnable = JSONParserUtilities.getBoolean(objectNode, "returnable")
            AdobeAdeptLoan(loanID, ByteBuffer.wrap(serialized), returnable)
        } else {
            null
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
