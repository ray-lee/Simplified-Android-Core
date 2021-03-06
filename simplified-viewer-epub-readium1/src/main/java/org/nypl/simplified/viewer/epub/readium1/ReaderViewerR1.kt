package org.nypl.simplified.viewer.epub.readium1

import android.app.Activity
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class ReaderViewerR1 : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(ReaderViewerR1::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.epub.readium1.ReaderViewerR1"

  override fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatPDF,
      is BookFormat.BookFormatAudioBook ->
        false

      is BookFormat.BookFormatEPUB -> {
        val r2Enabled = preferences.flags["useExperimentalR2"] ?: false
        return if (r2Enabled) {
          this.logger.warn("useExperimentalR2 is enabled, so R1 is disabled for EPUBs")
          false
        } else {
          this.logger.warn("useExperimentalR2 is disabled, so R1 is enabled for EPUBs")
          true
        }
      }
    }
  }

  override fun canPotentiallySupportType(type: MIMEType): Boolean {
    return type.fullType == "application/epub+zip"
  }

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val formatEPUB = format as BookFormat.BookFormatEPUB
    ReaderActivity.startActivity(
      activity,
      book.id,
      formatEPUB.file,
      FeedEntry.FeedEntryOPDS(book.account, book.entry)
    )
  }
}
