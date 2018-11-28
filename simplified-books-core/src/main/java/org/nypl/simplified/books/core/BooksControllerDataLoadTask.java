package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

final class BooksControllerDataLoadTask implements Callable<Unit> {
  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerDataLoadTask.class);

  private final BookDatabaseType books_database;
  private final BooksStatusCacheType books_status;
  private final AccountDataLoadListenerType listener;
  private final AccountsDatabaseReadableType accounts;
  private final boolean needs_auch;

  BooksControllerDataLoadTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final AccountsDatabaseReadableType in_accounts_database,
    final AccountDataLoadListenerType in_listener,
    final boolean in_needs_auch) {
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.listener = NullCheck.notNull(in_listener);
    this.accounts = NullCheck.notNull(in_accounts_database);
    this.needs_auch = in_needs_auch;
  }

  @Override
  public Unit call() throws Exception {
    final OptionType<AccountCredentials> credentials_opt =
      this.accounts.accountGetCredentials();

    if (credentials_opt.isSome() || !this.needs_auch) {
      this.books_database.databaseNotifyAllBookStatus(
        this.books_status,
        p -> {
          final BookID id = p.getLeft();
          final BookDatabaseEntrySnapshot snap = p.getRight();
          this.listener.onAccountDataBookLoadSucceeded(id, snap);
        },
        p -> {
          final Throwable e = p.getRight();
          final OptionType<Throwable> ex = Option.some(e);
          final BookID id = p.getLeft();
          this.listener.onAccountDataBookLoadFailed(id, ex, e.getMessage());
        });

    } else {
      try {
        this.listener.onAccountUnavailable();
      } catch (final Throwable x) {
        BooksControllerDataLoadTask.LOG.error(
          "listener raised exception: ", x);
      }
    }

    this.listener.onAccountDataBookLoadFinished();
    return Unit.unit();
  }
}
