package org.nypl.simplified.books.borrowing.internal

import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType

/**
 * The default implementation of the [BorrowSubtaskDirectoryType] interface.
 */

class BorrowSubtaskDirectory : BorrowSubtaskDirectoryType {
  override val subtasks =
    listOf(
      BorrowACSM,
      BorrowAxisNow,
      BorrowAudioBook,
      BorrowBearerToken,
      BorrowCopy,
      BorrowSAMLDownload,
      BorrowDirectDownload,
      BorrowLoanCreate
    )
}
