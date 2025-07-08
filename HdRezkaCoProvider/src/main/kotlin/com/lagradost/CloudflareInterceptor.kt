package com.lagradost

import com.lagradost.api.Log
import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup

class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller) : Interceptor {
    init {
        Log.d("CloudflareInterceptor","init CloudflareInterceptor")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val doc = Jsoup.parse(response.peekBody(10 * 1024).string())
        Log.d("CloudflareInterceptor", doc.text())
        if (response.code == 503 || doc.selectFirst("meta[name='cloudflare']") != null) {
            return cloudflareKiller.intercept(chain)
        }

        return response
    }
}