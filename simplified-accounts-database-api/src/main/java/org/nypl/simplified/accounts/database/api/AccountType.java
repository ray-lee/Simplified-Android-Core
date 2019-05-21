package org.nypl.simplified.accounts.database.api;

import org.nypl.simplified.accounts.api.AccountLoginState;
import org.nypl.simplified.accounts.api.AccountPreferences;
import org.nypl.simplified.accounts.api.AccountReadableType;
import org.nypl.simplified.books.book_database.api.BookDatabaseType;

/**
 * <p>The interface exposed by accounts.</p>
 *
 * <p>An account aggregates a set of credentials and a book database.
 * Account are assigned monotonically increasing identifiers by the
 * application, but the identifiers themselves carry no meaning. It is
 * an error to depend on the values of identifiers for any kind of
 * program logic.</p>
 */

public interface AccountType extends AccountReadableType {

  /**
   * @return The book database owned by this account
   */

  BookDatabaseType bookDatabase();

  /**
   * Set the current login state for the account.
   *
   * @param state The login state
   * @throws AccountsDatabaseException On database errors
   */

  void setLoginState(AccountLoginState state)
    throws AccountsDatabaseException;

  /**
   * Update the account preferences.
   *
   * @param preferences The new preferences
   * @throws AccountsDatabaseException On database errors
   */

  void setPreferences(AccountPreferences preferences)
    throws AccountsDatabaseException;
}