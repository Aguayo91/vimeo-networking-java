/*
 * Copyright (c) 2020 Vimeo (https://vimeo.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vimeo.networking2.config

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.vimeo.networking2.ApiConstants
import com.vimeo.networking2.internal.ErrorHandlingCallAdapterFactory
import com.vimeo.networking2.internal.interceptor.AcceptHeaderInterceptor
import com.vimeo.networking2.internal.interceptor.CacheControlHeaderInterceptor
import com.vimeo.networking2.internal.interceptor.LanguageHeaderInterceptor
import com.vimeo.networking2.internal.interceptor.UserAgentHeaderInterceptor
import com.vimeo.networking2.internal.params.StringValueJsonAdapterFactory
import com.vimeo.networking2.internal.params.VimeoParametersConverterFactory
import com.vimeo.networking2.logging.VimeoLogger
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Module which provides all the dependencies needed for setting up [Retrofit].
 */
object RetrofitSetupModule {

    /**
     * Create a Moshi instance used for serialization and deserialization.
     */
    @JvmStatic
    fun moshi(): Moshi = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(StringValueJsonAdapterFactory())
        .build()

    /**
     * Creates the object graph for the setup dependencies. After the graph is created, the method
     * return an instance of [Retrofit] which is root of the graph.
     */
    @JvmStatic
    fun retrofit(vimeoApiConfiguration: VimeoApiConfiguration): Retrofit {
        val interceptors = mutableListOf(
            UserAgentHeaderInterceptor(vimeoApiConfiguration.compositeUserAgent),
            AcceptHeaderInterceptor(),
            LanguageHeaderInterceptor(vimeoApiConfiguration.locales)
        )
        interceptors.addAll(vimeoApiConfiguration.applicationInterceptors)

        val networkInterceptors = listOf(CacheControlHeaderInterceptor(vimeoApiConfiguration.cacheMaxAgeSeconds))

        val okHttpClient = okHttpClient(vimeoApiConfiguration, interceptors, networkInterceptors)
        val moshi = moshi()
        return createRetrofit(vimeoApiConfiguration, okHttpClient, moshi)
    }

    /**
     * Clear the request cache associated with the [VimeoApiConfiguration] of all cache entries.
     */
    @JvmStatic
    fun clearRequestCache(vimeoApiConfiguration: VimeoApiConfiguration) {
        vimeoApiConfiguration.cache?.evictAll()
    }

    /**
     * Create [OkHttpClient] with interceptors and timeoutSeconds configurations.
     */
    private fun okHttpClient(
        vimeoApiConfiguration: VimeoApiConfiguration,
        applicationInterceptors: List<Interceptor>,
        networkInterceptors: List<Interceptor>
    ): OkHttpClient = OkHttpClient.Builder().apply {
        vimeoApiConfiguration.networkInterceptors.forEach { addNetworkInterceptor(it) }
        vimeoApiConfiguration.applicationInterceptors.forEach { addInterceptor(it) }

        connectTimeout(vimeoApiConfiguration.requestTimeoutSeconds, TimeUnit.SECONDS)
        readTimeout(vimeoApiConfiguration.requestTimeoutSeconds, TimeUnit.SECONDS)
        writeTimeout(vimeoApiConfiguration.requestTimeoutSeconds, TimeUnit.SECONDS)
        retryOnConnectionFailure(false)

        if (vimeoApiConfiguration.cache != null) {
            cache(vimeoApiConfiguration.cache)
        }

        interceptors().addAll(applicationInterceptors)
        networkInterceptors().addAll(networkInterceptors)

        if (vimeoApiConfiguration.isCertPinningEnabled) {
            certificatePinner(createCertificatePinner())
        }
    }.build()

    /**
     * Create certificate pinner to configure OkHttpClient.
     */
    private fun createCertificatePinner() = CertificatePinner.Builder()
        .add(ApiConstants.SSL_URL_PATTERN, ApiConstants.SSL_PUBLIC_KEY)
        .build()

    /**
     * Create [Retrofit] with OkHttpClient and Moshi.
     */
    private fun createRetrofit(
        vimeoApiConfiguration: VimeoApiConfiguration,
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(vimeoApiConfiguration.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(VimeoParametersConverterFactory())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(ErrorHandlingCallAdapterFactory(VimeoLogger(
            vimeoApiConfiguration.logDelegate,
            vimeoApiConfiguration.logLevel
        )))
        .build()

    /**
     * Creates a cache for storing Retrofit services.
     */
    @JvmStatic
    fun retrofitCache(retrofit: Retrofit) = RetrofitServicesCache(retrofit)

}
