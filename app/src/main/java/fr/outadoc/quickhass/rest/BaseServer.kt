package fr.outadoc.quickhass.rest

import fr.outadoc.quickhass.preferences.PreferenceRepository
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


abstract class BaseServer<T>(
    private val type: Class<T>,
    private val prefs: PreferenceRepository
) {
    private val baseUri: HttpUrl?
        get() = HttpUrl.parse(prefs.instanceBaseUrl)

    private val altBaseUri: HttpUrl?
        get() = if (prefs.altInstanceBaseUrl != null) HttpUrl.parse(prefs.altInstanceBaseUrl!!) else null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val newHeaders = chain.request()
            .headers()
            .newBuilder()
            .set("Authorization", "Bearer ${prefs.accessToken}")
            .build()

        val newRequest = chain.request()
            .newBuilder()
            .headers(newHeaders)
            .build()

        chain.proceed(newRequest)
    }

    private val altBaseUrlInterceptor = Interceptor { chain ->
        val req = chain.request()
        val res = chain.proceed(req)

        if (res.isSuccessful && altBaseUri != null) {
            res
        } else {
            val oldUrl = req.url().toString()
            val newUrl = HttpUrl.parse(oldUrl.replace(baseUri.toString(), baseUri.toString()))

            if (newUrl != null && newUrl != req.url()) {
                val newRequest = chain.request()
                    .newBuilder()
                    .url(newUrl)
                    .build()

                chain.proceed(newRequest)
            } else {
                res
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(altBaseUrlInterceptor)
        .build()

    private val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(baseUri.toString())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    val api: T
        get() = retrofit.create(type)
}