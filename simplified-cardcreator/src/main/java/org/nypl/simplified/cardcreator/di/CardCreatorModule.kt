package org.nypl.simplified.cardcreator.di

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Constants
import org.nypl.simplified.cardcreator.viewmodels.AddressViewModel
import org.nypl.simplified.cardcreator.viewmodels.PatronViewModel
import org.nypl.simplified.cardcreator.viewmodels.UsernameViewModel
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


val cardCreatorModule = module {

  // retorfit service
  single { (authUsername: String, authPassword: String) ->

    val logging = run {
      val httpLoggingInterceptor = HttpLoggingInterceptor()
      httpLoggingInterceptor.apply {
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
      }
    }

    val auth = Interceptor {
      val request = it.request().newBuilder()
        .addHeader("Authorization",
          Credentials.basic(
            authUsername,
            authPassword))
        .build()
      it.proceed(request)
    }

    val client: OkHttpClient = OkHttpClient.Builder()
      .addInterceptor(logging)
      .addInterceptor(auth)
      .build()

    Retrofit.Builder()
      .client(client)
      .baseUrl(Constants.LIBRARY_SIMPLIFIED_BASE_URL)
      .addConverterFactory(MoshiConverterFactory.create())
      .build()
  }

  single { (authUsername: String, authPassword: String) ->
    CardCreatorService(authUsername, authPassword)
  }

  // view model
  viewModel { UsernameViewModel() }
  viewModel { AddressViewModel() }
  viewModel { PatronViewModel() }
}

