package org.nypl.simplified.webview

import android.webkit.CookieManager
import org.nypl.simplified.accounts.api.AccountCookie
import java.io.File

/**
 * Web view utility functions.
 */

object WebViewUtilities {

  /**
   * Dump cookies from the Android web view as a list of account cookies.
   */

  fun dumpCookiesAsAccountCookies(
    dataDir: File
  ): List<AccountCookie> {
    CookieManager.getInstance().flush()

    return WebViewCookieDatabase(dataDir).use {
      it.getAll().map { webViewCookie ->
        val domain = webViewCookie.hostKey.trimStart('.')

        AccountCookie(
          url = (if (webViewCookie.isSecure > 0) "https" else "http") + "://$domain",
          value = webViewCookie.toSetCookieString()
        )
      }
    }
  }
}
