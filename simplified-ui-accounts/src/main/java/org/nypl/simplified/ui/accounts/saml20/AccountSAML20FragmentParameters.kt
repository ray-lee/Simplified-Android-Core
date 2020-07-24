package org.nypl.simplified.ui.accounts.saml20

import java.io.Serializable
import java.net.URI

data class AccountSAML20FragmentParameters(
  val authenticationURI: URI
): Serializable
