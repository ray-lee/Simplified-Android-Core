package org.nypl.simplified.ui.accounts.saml20

import android.content.res.Resources
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.R
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * View state for the SAML 2.0 fragment.
 */

class AccountSAML20ViewModel(
  private val profiles: ProfilesControllerType,
  private val account: AccountID,
  private val description: AccountProviderAuthenticationDescription.SAML2_0,
  private val resources: Resources
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(AccountSAML20ViewModel::class.java)
  private val eventSubject =
    PublishSubject.create<AccountSAML20Event>()
  private val authInfo =
    AtomicReference<AuthInfo>()

  private data class AuthInfo(
    val token: String,
    val patronInfo: String,
    val cookies: Set<String>
  )

  val events: Observable<AccountSAML20Event> =
    this.eventSubject

  private class AccountSAML20WebClient(
    private val logger: Logger,
    private val resources: Resources,
    private val eventSubject: PublishSubject<AccountSAML20Event>,
    private val authInfo: AtomicReference<AuthInfo>,
    private val profiles: ProfilesControllerType,
    private val account: AccountID
  ) : WebViewClient() {

    private val cookies = mutableSetOf<String>()

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
      /*
       * We don't know which cookies we need, so we keep them all.
       */

      val cookieManager = CookieManager.getInstance()
      val cookie = cookieManager.getCookie(url)
      if (cookie != null) {
        this.cookies.add(cookie)
      }

      if (url.startsWith(AccountSAML20.callbackURI)) {
        val parsed = Uri.parse(url)

        val accessToken = parsed.getQueryParameter("access_token")
        if (accessToken == null) {
          val message = this.resources.getString(R.string.accountSAML20NoAccessToken)
          this.logger.error("{}", message)
          this.eventSubject.onNext(AccountSAML20Event.Failed(message))
          return true
        }

        val patronInfo = parsed.getQueryParameter("patron_info")
        if (patronInfo == null) {
          val message = this.resources.getString(R.string.accountSAML20NoPatronInfo)
          this.logger.error("{}", message)
          this.eventSubject.onNext(AccountSAML20Event.Failed(message))
          return true
        }

        this.logger.debug("obtained access token")
        this.authInfo.set(
          AuthInfo(
            token = accessToken,
            patronInfo = patronInfo,
            cookies = this.cookies.toSet()
          )
        )

        this.profiles.profileAccountLogin(
          ProfileAccountLoginRequest.SAML20Complete(
            accountId = this.account,
            accessToken = accessToken,
            patronInfo = patronInfo,
            cookies = this.cookies.toSet()
          )
        )
        this.eventSubject.onNext(
          AccountSAML20Event.AccessTokenObtained(
            token = accessToken,
            patronInfo = patronInfo,
            cookies = cookies
          )
        )
        return true
      }
      return false
    }
  }

  override fun onCleared() {
    super.onCleared()

    if (this.authInfo.get() == null) {
      this.logger.debug("no access token obtained; cancelling login")
      this.profiles.profileAccountLogin(
        ProfileAccountLoginRequest.SAML20Cancel(
          accountId = this.account,
          description = this.description
        )
      )
    }

    this.eventSubject.onComplete()
  }

  val webViewClient: WebViewClient =
    AccountSAML20WebClient(
      account = this.account,
      eventSubject = this.eventSubject,
      logger = this.logger,
      profiles = this.profiles,
      resources = this.resources,
      authInfo = this.authInfo
    )
}
