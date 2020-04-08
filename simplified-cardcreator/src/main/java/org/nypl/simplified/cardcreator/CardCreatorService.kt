package org.nypl.simplified.cardcreator

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import org.koin.core.context.loadKoinModules
import org.nypl.simplified.cardcreator.di.cardCreatorModule
import java.io.InputStream
import java.util.Properties

class CardCreatorService(private val username: String, private val password: String) : CardCreatorServiceType {

  companion object {
    fun create(configFile: InputStream): CardCreatorServiceType {
      val properties = Properties()
      properties.load(configFile)
      val username = properties.getProperty("cardcreator.prod.username")
      val password = properties.getProperty("cardcreator.prod.password")


      loadKoinModules(cardCreatorModule)
      return CardCreatorService(username, password)
    }
  }

  override fun openCardCreatorActivity(fragment: Fragment, context: Context?, resultCode: Int) {
    val intent = Intent(context, CardCreatorActivity::class.java)
    intent.putExtra("username", username)
    intent.putExtra("password", password)
    fragment.startActivityForResult(intent, resultCode)
  }
}
