package org.nypl.simplified.ui.accounts.saml20

import android.content.res.Resources
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.ui.accounts.R

class AccountSAML20ViewModel(
  val resources: Resources
) : ViewModel() {

  private val eventSubject =
    PublishSubject.create<AccountSAML20Event>()

  private class AccountSAML20WebClient(
    val resources: Resources,
    val eventSubject: PublishSubject<AccountSAML20Event>
  ) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
      view: WebView,
      url: String
    ): Boolean {
      return this.handleUrl(view, url)
    }

    override fun shouldOverrideUrlLoading(
      view: WebView,
      request: WebResourceRequest
    ): Boolean {
      return this.handleUrl(view, request.url.toString())
    }

    private fun handleUrl(
      view: WebView,
      url: String
    ): Boolean {
      if (url.startsWith(AccountSAML20.callbackURI)) {
        val parsed = Uri.parse(url)
        val accessToken = parsed.getQueryParameter("access_token")

        if (accessToken == null) {
          val message = this.resources.getString(R.string.accountSAML20NoAccessToken)
          this.eventSubject.onNext(AccountSAML20Event.Failed(message))
          return true
        }

        this.eventSubject.onNext(AccountSAML20Event.AccessTokenObtained(accessToken))
        return true
      }
      return false
    }
  }

  val webViewClient: WebViewClient =
    AccountSAML20WebClient(
      resources = this.resources,
      eventSubject = this.eventSubject
    )
}
