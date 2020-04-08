package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.parameter.parametersOf
import org.nypl.simplified.cardcreator.models.Username
import org.nypl.simplified.cardcreator.models.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class UsernameViewModel : ViewModel(), KoinComponent {

  private val logger = LoggerFactory.getLogger(UsernameViewModel::class.java)

  val validateUsernameResponse = MutableLiveData<ValidateUsernameResponse>()

  fun validateUsername(username: String, authUsername: String, authPassword: String) {
    viewModelScope.launch {
      try {
        val cardCreatorService: CardCreatorService = get { parametersOf(authUsername, authPassword) }
        val response = cardCreatorService.validateUsername(Username(username))
        validateUsernameResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("validateUsername call failed!")
        e.printStackTrace()
      }
    }
  }

}
