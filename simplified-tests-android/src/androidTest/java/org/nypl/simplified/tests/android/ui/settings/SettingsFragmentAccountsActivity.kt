package org.nypl.simplified.tests.android.ui.settings

import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.accounts.AccountListFragment
import org.nypl.simplified.ui.accounts.AccountsFragmentParameters

class SettingsFragmentAccountsActivity : NavigationHostActivity<AccountListFragment>() {
  override fun createFragment(): AccountListFragment =
    AccountListFragment.create(
      AccountsFragmentParameters(
        shouldShowLibraryRegistryMenu = true
      )
    )

  override fun serviceDirectory(): ServiceDirectoryType {
    return Services.serviceDirectory()
  }
}
