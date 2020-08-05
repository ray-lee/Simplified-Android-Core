package org.nypl.simplified.ui.accounts.saml20

/**
 * Events raised during the SAML login process.
 */

sealed class AccountSAML20Event {

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
    val cookies: Set<String>
  ) : AccountSAML20Event()
}
