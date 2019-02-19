package org.nypl.simplified.books.reader.bookmarks

import java.lang.IllegalArgumentException

/**
 * The kind of bookmarks.
 */

sealed class ReaderBookmarkKind(
  val motivationURI: String) {

  /**
   * The bookmark represents a last-read location.
   */

  object ReaderBookmarkLastReadLocation :
    ReaderBookmarkKind("http://librarysimplified.org/terms/annotation/idling")

  /**
   * The bookmark represents an explicitly created bookmark.
   */

  object ReaderBookmarkExplicit :
    ReaderBookmarkKind("http://www.w3.org/ns/oa#bookmarking")


  companion object {

    fun ofMotivation(motivationURI: String): ReaderBookmarkKind {
      return when (motivationURI) {
        ReaderBookmarkLastReadLocation.motivationURI -> ReaderBookmarkLastReadLocation
        ReaderBookmarkExplicit.motivationURI -> ReaderBookmarkExplicit
        else -> throw IllegalArgumentException("Unrecognized motivation: $motivationURI")
      }
    }

  }
}
