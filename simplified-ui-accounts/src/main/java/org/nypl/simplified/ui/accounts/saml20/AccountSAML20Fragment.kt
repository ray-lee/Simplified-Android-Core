package org.nypl.simplified.ui.accounts.saml20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import org.nypl.simplified.ui.accounts.R

class AccountSAML20Fragment : Fragment() {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountSAML20FragmentParameters): AccountSAML20Fragment {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = AccountSAML20Fragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var viewModel: AccountSAML20ViewModel
  private lateinit var parameters: AccountSAML20FragmentParameters
  private lateinit var webView: WebView

  private fun constructLoginURI(): String {
    return buildString {
      this.append(this@AccountSAML20Fragment.parameters.authenticationURI)
      this.append("&redirect_uri=")
      this.append(AccountSAML20.callbackURI)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters = this.requireArguments()[PARAMETERS_ID] as AccountSAML20FragmentParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout = inflater.inflate(R.layout.account_saml20, container, false)
    this.webView = layout.findViewById(R.id.saml20WebView)
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.viewModel =
      ViewModelProviders.of(this, AccountSAML20ViewModelFactory(this.resources))
        .get(AccountSAML20ViewModel::class.java)

    this.webView.webViewClient = this.viewModel.webViewClient
    this.webView.loadUrl(this.constructLoginURI())
  }
}
