package org.nypl.simplified.ui.accounts.saml20

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AccountSAML20ViewModelFactory(
  val resources: Resources
) : ViewModelProvider.Factory {

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == AccountSAML20ViewModel::class.java) {
      return AccountSAML20ViewModel(this.resources) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
