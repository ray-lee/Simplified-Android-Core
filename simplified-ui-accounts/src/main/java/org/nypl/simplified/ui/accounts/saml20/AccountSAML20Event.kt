package org.nypl.simplified.ui.accounts.saml20

sealed class AccountSAML20Event {

  data class Failed(
    val message: String
  ): AccountSAML20Event()

  data class AccessTokenObtained(
    val token: String
  ): AccountSAML20Event()
}
