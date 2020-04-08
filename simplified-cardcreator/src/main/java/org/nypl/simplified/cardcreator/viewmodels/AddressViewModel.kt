package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.parameter.parametersOf
import org.nypl.simplified.cardcreator.models.Address
import org.nypl.simplified.cardcreator.models.ValidateAddressResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class AddressViewModel : ViewModel(), KoinComponent {

  private val logger = LoggerFactory.getLogger(AddressViewModel::class.java)

  val validateAddressResponse = MutableLiveData<ValidateAddressResponse>()

  fun validateAddress(address: Address, authUsername: String, authPassword: String) {
    viewModelScope.launch {
      try {
        val cardCreatorService: CardCreatorService = get { parametersOf(authUsername, authPassword) }
        val response = cardCreatorService.validateAddress(address)
        validateAddressResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("validateAddress call failed!")
        e.printStackTrace()
      }
    }
  }

}
