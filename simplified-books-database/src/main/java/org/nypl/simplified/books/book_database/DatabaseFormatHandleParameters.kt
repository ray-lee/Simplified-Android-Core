package org.nypl.simplified.books.book_database

import android.content.Context
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import java.io.File

/**
 * Parameters passed to database format handles.
 */

internal data class DatabaseFormatHandleParameters(

  /**
   * An Android context.
   */

  val context: Context,

  /**
   * The ID of the book to which the owning database entry belongs.
   */

  val bookID: org.nypl.simplified.books.api.BookID,

  /**
   * The directory containing data for the database entry.
   */

  val directory: File,

  /**
   * A callback to be executed whenever something causes the contents of a format handle
   * to change. In practice, this is used by the book database to update its internal snapshots
   * of book states.
   */

  val onUpdated: (org.nypl.simplified.books.api.BookFormat) -> Unit,

  /**
   * The database entry that owns the format handle.
   */

  val entry: org.nypl.simplified.books.book_database.api.BookDatabaseEntryType)