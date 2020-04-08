package org.nypl.simplified.cardcreator.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.nypl.simplified.cardcreator.di.cardCreatorModule
import org.nypl.simplified.cardcreator.models.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.CoroutinesTestRule

@RunWith(JUnit4::class)
class UsernameViewModelTest : KoinTest {

  // to test live data
  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  // to test coroutines
  @get:Rule
  var coroutinesTestRule = CoroutinesTestRule()

  // needed to observe live data
  @Mock
  lateinit var observer: Observer<ValidateUsernameResponse>

  @Mock
  lateinit var lifecycleOwner: LifecycleOwner

  lateinit var lifecycle: Lifecycle
  lateinit var viewModel: UsernameViewModel

  @Before
  @Throws(Exception::class)
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    lifecycle = LifecycleRegistry(lifecycleOwner)

    startKoin {
      modules(cardCreatorModule)
    }

  }

  @Test
  fun shouldReturnCorrectResponse() = coroutinesTestRule.testDispatcher.runBlockingTest {
    val expectedResponse = ValidateUsernameResponse(type = "available-username", card_type = "standard", message = "This username is available.")

    val creatorService: CardCreatorService = mock {
      whenever(mock.validateUsername(any())) doReturn expectedResponse
    }

    val testModule = module(override = true) {
      single {
        creatorService
      }
    }

    loadKoinModules(testModule)

    viewModel = UsernameViewModel()
    viewModel.validateUsernameResponse.observeForever(observer)

    viewModel.validateUsername("testuser", "9w8293834", "testPasw")

    verify(observer).onChanged(expectedResponse);
  }

  @After
  fun tearDown() {
    stopKoin()
    Dispatchers.resetMain()
  }

}
