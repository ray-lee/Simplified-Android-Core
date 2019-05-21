package org.nypl.simplified.tests.books.accounts;

import android.content.Context;

import com.io7m.jfunctional.Option;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountBarcode;
import org.nypl.simplified.accounts.api.AccountEvent;
import org.nypl.simplified.accounts.api.AccountLoginState;
import org.nypl.simplified.accounts.api.AccountPIN;
import org.nypl.simplified.accounts.api.AccountProvider;
import org.nypl.simplified.accounts.api.AccountProviderCollectionType;
import org.nypl.simplified.accounts.database.AccountProviderCollection;
import org.nypl.simplified.accounts.database.AccountsDatabase;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType;
import org.nypl.simplified.books.book_database.BookDatabases;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.observable.Observable;
import org.nypl.simplified.observable.ObservableType;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class AccountsDatabaseContract {

  private static final Logger LOG = LoggerFactory.getLogger(AccountsDatabaseContract.class);

  private ObservableType<AccountEvent> accountEvents;
  private ObservableType<ProfileEvent> profileEvents;
  private FakeAccountCredentialStorage credentialStore;

  protected abstract Context context();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setup()
  {
    this.credentialStore = new FakeAccountCredentialStorage();
    this.accountEvents = Observable.create();
    this.profileEvents = Observable.create();
  }

  /**
   * An exception matcher that checks to see if the given accounts database exception has
   * at least one cause of the given type and with the given exception message.
   *
   * @param <T> The cause type
   */

  private static final class CausesContains<T extends Exception>
    extends BaseMatcher<AccountsDatabaseException> {
    private final Class<T> exception_type;
    private final String message;

    CausesContains(
      final Class<T> exception_type,
      final String message) {
      this.exception_type = exception_type;
      this.message = message;
    }

    @Override
    public boolean matches(final Object item) {
      if (item instanceof AccountsDatabaseException) {
        final AccountsDatabaseException ex = (AccountsDatabaseException) item;
        for (final Exception c : ex.causes()) {
          LOG.error("Cause: ", c);
          if (exception_type.isAssignableFrom(c.getClass()) && c.getMessage().contains(message)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("must throw AccountsDatabaseException");
      description.appendText(" with at least one cause of type " + exception_type);
      description.appendText(" with a message containing '" + message + "'");
    }
  }

  @Test
  public final void testOpenExistingNotDirectory()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");

    FileUtilities.fileWriteUTF8(f_acc, "Hello!");

    expected.expect(AccountsDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Not a directory"));
    AccountsDatabase.open(
      context(),
      this.accountEvents,
      bookDatabases(),
      accountProviders(),
      this.credentialStore,
      f_acc);
  }

  private BookDatabases bookDatabases() {
    return BookDatabases.INSTANCE;
  }

  /**
   * A bad subdirectory will be migrated but will still fail to open.
   */

  @Test
  public final void testOpenExistingBadSubdirectory()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");
    f_acc.mkdirs();
    final File f_a = new File(f_acc, "xyz");
    f_a.mkdirs();

    expected.expect(AccountsDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Could not parse account: "));
    AccountsDatabaseType database =
      AccountsDatabase.open(
      context(),
      this.accountEvents,
      bookDatabases(),
      accountProviders(),
      this.credentialStore,
      f_acc);
  }

  @Test
  public final void testOpenExistingJSONMissing()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");
    f_acc.mkdirs();
    final File f_a = new File(f_acc, "64e606e8-e242-4c5f-9bfb-63df11b187bd");
    f_a.mkdirs();

    expected.expect(AccountsDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Could not parse account: "));
    AccountsDatabase.open(
      context(),
      this.accountEvents,
      bookDatabases(),
      accountProviders(),
      this.credentialStore,
      f_acc);
  }

  @Test
  public final void testOpenExistingJSONUnparseable()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");
    f_acc.mkdirs();
    final File f_a = new File(f_acc, "0");
    f_a.mkdirs();

    final File f_f = new File(f_a, "account.json");
    FileUtilities.fileWriteUTF8(f_f, "} { this is not JSON { } { }");

    expected.expect(AccountsDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Could not parse account: "));
    AccountsDatabase.open(
      context(),
      this.accountEvents,
      bookDatabases(),
      accountProviders(),
      this.credentialStore,
      f_acc);
  }

  @Test
  public final void testOpenExistingEmpty()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");

    final AccountsDatabaseType db =
      AccountsDatabase.open(
        context(),
        this.accountEvents,
        bookDatabases(),
        accountProviders(),
        this.credentialStore,
        f_acc);

    Assert.assertEquals(0, db.accounts().size());
    Assert.assertEquals(f_acc, db.directory());
  }

  @Test
  public final void testCreateAccount()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");

    final AccountProviderCollectionType account_providers =
      accountProviders();
    final AccountsDatabaseType db =
      AccountsDatabase.open(
        context(),
        this.accountEvents,
        bookDatabases(),
        account_providers,
        this.credentialStore,
        f_acc);

    final AccountProvider provider0 =
      fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider provider1 =
      fakeProvider("http://www.example.com/accounts1/");

    final AccountType acc0 = db.createAccount(provider0);
    final AccountType acc1 = db.createAccount(provider1);

    Assert.assertTrue("Account 0 directory exists", acc0.directory().isDirectory());
    Assert.assertTrue("Account 1 directory exists", acc1.directory().isDirectory());

    Assert.assertTrue(
      "Account 0 file exists",
      new File(acc0.directory(), "account.json").isFile());
    Assert.assertTrue(
      "Account 1 file exists",
      new File(acc1.directory(), "account.json").isFile());

    Assert.assertEquals(
      account_providers.provider(URI.create("http://www.example.com/accounts0/")),
      acc0.provider());
    Assert.assertEquals(
      account_providers.provider(URI.create("http://www.example.com/accounts1/")),
      acc1.provider());

    Assert.assertNotEquals(acc0.id(), acc1.id());
    Assert.assertNotEquals(acc0.directory(), acc1.directory());

    Assert.assertEquals(AccountLoginState.AccountNotLoggedIn.INSTANCE, acc0.loginState());
    Assert.assertEquals(AccountLoginState.AccountNotLoggedIn.INSTANCE, acc1.loginState());
  }

  @Test
  public final void testCreateReopen()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");

    final AccountsDatabaseType db0 =
      AccountsDatabase.open(
        context(),
        this.accountEvents,
        bookDatabases(),
        accountProviders(),
        this.credentialStore,
        f_acc);

    final AccountProvider provider0 =
      fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider provider1 =
      fakeProvider("http://www.example.com/accounts1/");

    final AccountType acc0 = db0.createAccount(provider0);
    final AccountType acc1 = db0.createAccount(provider1);

    final AccountsDatabaseType db1 =
      AccountsDatabase.open(
        context(),
        this.accountEvents,
        bookDatabases(),
        accountProviders(),
        this.credentialStore,
        f_acc);

    final AccountType acr0 = db1.accounts().get(acc0.id());
    final AccountType acr1 = db1.accounts().get(acc1.id());

    Assert.assertEquals(acc0.id(), acr0.id());
    Assert.assertEquals(acc1.id(), acr1.id());
    Assert.assertEquals(acc0.directory(), acr0.directory());
    Assert.assertEquals(acc1.directory(), acr1.directory());
    Assert.assertEquals(acc0.provider(), acr0.provider());
    Assert.assertEquals(acc1.provider(), acr1.provider());
  }

  @Test
  public final void testSetCredentials()
    throws Exception {

    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    f_pro.mkdirs();
    final File f_p = new File(f_pro, "0");
    f_p.mkdirs();
    final File f_acc = new File(f_p, "accounts");

    final AccountsDatabaseType db0 =
      AccountsDatabase.open(
        context(),
        this.accountEvents,
        bookDatabases(),
        accountProviders(),
        this.credentialStore,
        f_acc);

    final AccountProvider provider0 = fakeProvider("http://www.example.com/accounts0/");
    final AccountType acc0 = db0.createAccount(provider0);

    final AccountAuthenticationCredentials creds =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("1234"),
        AccountBarcode.create("1234"))
        .build();

    acc0.setLoginState(new AccountLoginState.AccountLoggedIn(creds));
    Assert.assertEquals(new AccountLoginState.AccountLoggedIn(creds), acc0.loginState());
  }

  private AccountProviderCollectionType accountProviders() {
    final AccountProvider p0 = fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider p1 = fakeProvider("http://www.example.com/accounts1/");
    final SortedMap<URI, AccountProvider> providers = new TreeMap<>();
    providers.put(p0.id(), p0);
    providers.put(p1.id(), p1);
    return AccountProviderCollection.create(p0, providers);
  }

  private static AccountProvider fakeProvider(final String provider_id) {
    return AccountProvider.builder()
      .setId(URI.create(provider_id))
      .setDisplayName("Fake Library")
      .setSubtitle(Option.some("Imaginary books"))
      .setLogo(Option.some(URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo=")))
      .setCatalogURI(URI.create("http://example.com/accounts0/feed.xml"))
      .setSupportEmail("postmaster@example.com")
      .setAnnotationsURI(Option.some(URI.create("http://example.com/accounts0/annotations")))
      .setPatronSettingsURI(Option.some(URI.create("http://example.com/accounts0/patrons/me")))
      .build();
  }
}