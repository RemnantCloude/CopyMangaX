package com.crow.copymanga.model.di

import androidx.multidex.BuildConfig
import com.crow.mangax.copymanga.BaseStrings
import com.crow.mangax.copymanga.MangaXAccountConfig
import com.crow.mangax.copymanga.okhttp.AppProgressFactory
import com.crow.mangax.copymanga.okhttp.AppProgressResponseBody
import com.crow.base.tools.extensions.baseMoshi
import com.crow.base.tools.extensions.info
import com.crow.base.tools.extensions.log
import com.crow.base.tools.network.FlowCallAdapterFactory
import com.crow.mangax.copymanga.BaseStrings.URL
import com.crow.mangax.copymanga.entity.AppConfig
import com.crow.mangax.copymanga.entity.CatlogConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {

    val named_CopyMangaX = named("CopyMangaX")
    val named_HotMangaX = named("HotMangaX")
    val named_Progress = named("ProgressOkHttp")

    /**
     * ⦁ 默认Okhttp
     * ⦁ 2023-06-16 21:39:31 周五 下午
     */
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().also { interceptor ->
                interceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .sslSocketFactory(SSLSocketClient.sSLSocketFactory, SSLSocketClient.geX509tTrustManager())
            .hostnameVerifier(SSLSocketClient.hostnameVerifier)
            .pingInterval(10, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
        .build()
    }

    /**
     * ⦁ 默认Retrofit
     * ⦁ 2023-06-16 21:40:42 周五 下午
     */
    single {
        Retrofit.Builder()
            .baseUrl("https://gitlab.com/")
            .client(get())
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(baseMoshi))
            .build()
    }

    /**
     * ⦁ 进度加载 By Okhttp
     * ⦁ 2023-06-16 21:41:59 周五 下午
     */
    single(named_Progress) {
        OkHttpClient.Builder().apply {
            addInterceptor { chain ->
                val request = if (!CatlogConfig.mApiImageProxyEnable) {
                    chain.request()
                } else {
                    if ((AppConfig.getInstance()?.mApiSecret?.length ?: 0) >= 20) {
                        chain.request().newBuilder()
                            .addHeader("x-api-key", AppConfig.getInstance()?.mApiSecret ?: "")
                            .build()
                    } else {
                        chain.request()
                    }
                }
                val response = chain.proceed(request)
                val urlString = request.url.toString()
                val progressFactory = AppProgressFactory.getProgressFactory(urlString)
                if (progressFactory == null) {
                    response
                } else {
                    response.newBuilder().body(AppProgressResponseBody(urlString, progressFactory.mRequestProgressListener, response.body)).build()
                }
            }
            sslSocketFactory(SSLSocketClient.sSLSocketFactory, SSLSocketClient.geX509tTrustManager())
            hostnameVerifier(SSLSocketClient.hostnameVerifier)
            retryOnConnectionFailure(true)
        }.build()
    }

    /**
     * ⦁ CopyMangaX 站点 By Okhttp
     * ⦁ 2023-06-16 21:42:49 周五 下午
     */
    single(named_CopyMangaX) {
        OkHttpClient.Builder()

            // 动态请求地址
            .addInterceptor { chain ->
                val request = chain.request()
                val urlBuilder = if(!CatlogConfig.mApiProxyEnable) {
                    URL.COPYMANGA.toHttpUrl().newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()
                } else {
                    if ((AppConfig.getInstance()?.mApiSecret?.length ?: 0) >= 20) {
                        URL.WUYA_API_ROUTE.toHttpUrl().newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()
                    } else {
                        URL.COPYMANGA.toHttpUrl().newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()
                    }
                }
                chain.proceed(request.newBuilder().url(urlBuilder).build())
            }

            // 动态添加请求头
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->

                chain.proceed(chain.request().newBuilder()
                    .addHeader("User-Agent", "Kotlin/1.9.10 (kotlin:io)")
                    .addHeader("Platform", "1")
                    .addHeader("Authorization","Token ${MangaXAccountConfig.mAccountToken}")
                    .addHeader("region", MangaXAccountConfig.mRoute)
                    .addHeader("x-api-key", AppConfig.getInstance()?.mApiSecret ?: "")
                    .build()
                )
            })
            .sslSocketFactory(SSLSocketClient.sSLSocketFactory, SSLSocketClient.geX509tTrustManager())
            .hostnameVerifier(SSLSocketClient.hostnameVerifier)
            .pingInterval(5, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
        .build()
    }

    /**
     * ⦁ HotMangaX 站点 By Okhttp
     * ⦁ 2023-10-10 01:23:25 周二 上午
     */
    single(named_HotMangaX) {
        OkHttpClient.Builder()

            // 动态请求地址
            .addInterceptor { chain ->
                val request = chain.request()
                chain.proceed(request.newBuilder().url(URL.HotManga.toHttpUrl().newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()).build())
            }

            // 动态添加请求头
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(chain.request().newBuilder()
                    .addHeader("User-Agent", "Kotlin/1.9.10 (kotlin:io)")
                    .addHeader("Platform", "1")
                    .addHeader("Authorization","Token ${MangaXAccountConfig.mHotMangaToken}")
                    .addHeader("region", MangaXAccountConfig.mRoute)
                    .build()
                )
            })
            .hostnameVerifier(SSLSocketClient.hostnameVerifier)
            .pingInterval(5, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * ⦁ CopyMangaX 站点 By Retrofit
     * ⦁ 2023-06-16 21:43:36 周五 下午
     */
    single(named_CopyMangaX) {
        Retrofit.Builder()
            .baseUrl(URL.COPYMANGA)
            .client(get(named_CopyMangaX))
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(baseMoshi))
            .build()
    }

    /**
     * ⦁ CopyMangaX 站点 By Retrofit
     * ⦁ 2023-06-16 21:43:36 周五 下午
     */
    single(named_HotMangaX) {
        Retrofit.Builder()
            .baseUrl(URL.HotManga)
            .client(get(named_HotMangaX))
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(baseMoshi))
            .build()
    }
}
