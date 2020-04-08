package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.parameter.parametersOf
import org.nypl.simplified.cardcreator.models.CreatePatronResponse
import org.nypl.simplified.cardcreator.models.Patron
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class PatronViewModel : ViewModel(), KoinComponent {

  private val logger = LoggerFactory.getLogger(PatronViewModel::class.java)

  val createPatronResponse = MutableLiveData<CreatePatronResponse>()

  fun createPatron(patron: Patron, authUsername: String, authPassword: String) {
    viewModelScope.launch {
      try {
        val cardCreatorService: CardCreatorService = get { parametersOf(authUsername, authPassword) }
        val response = cardCreatorService.createPatron(patron)
        createPatronResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("createPatron call failed!")
        e.printStackTrace()
      }
    }
  }

}
