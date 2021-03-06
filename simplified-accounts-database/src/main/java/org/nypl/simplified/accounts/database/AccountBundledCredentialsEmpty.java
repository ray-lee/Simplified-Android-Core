package org.nypl.simplified.accounts.database;

import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * An empty set of bundled credentials.
 */

public final class AccountBundledCredentialsEmpty implements AccountBundledCredentialsType {

  private static final AccountBundledCredentialsEmpty INSTANCE =
    new AccountBundledCredentialsEmpty();

  private Map<URI, AccountAuthenticationCredentials> empty;

  /**
   * @return A reference to the credentials
   */

  public static AccountBundledCredentialsType getInstance() {
    return INSTANCE;
  }

  private AccountBundledCredentialsEmpty() {
    this.empty = Collections.unmodifiableMap(Collections.emptyMap());
  }

  @Override
  public Map<URI, AccountAuthenticationCredentials> bundledCredentials() {
    return this.empty;
  }

  @Override
  public AccountAuthenticationCredentials bundledCredentialsFor(URI accountProvider) {
    return null;
  }
}
