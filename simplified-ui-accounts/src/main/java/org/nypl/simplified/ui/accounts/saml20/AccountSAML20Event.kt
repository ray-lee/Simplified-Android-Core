package org.nypl.simplified.ui.accounts.saml20

import org.nypl.simplified.accounts.api.AccountCookie

/**
 * Events raised during the SAML login process.
 */

sealed class AccountSAML20Event {

  class WebViewClientReady() : AccountSAML20Event()

  /**
   * The process failed.
   */

  data class Failed(
    val message: String
  ) : AccountSAML20Event()

  /**
   * An access token was obtained.
   */

  data class AccessTokenObtained(
    val token: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>
  ) : AccountSAML20Event()
}
