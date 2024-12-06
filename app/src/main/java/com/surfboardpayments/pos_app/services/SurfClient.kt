package com.surfboardpayments.pos_app.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.okhttp.Headers
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONObject

enum class RequestType {
    POST, GET, DELETE
}

val serializer: Gson = GsonBuilder().setLenient().disableHtmlEscaping().create()

class SurfClient() {
    companion object {
        private var baseUrl: String = ""
        private var _initialise: Boolean = false
        private var _authToken: String = ""
        /**
         * Use the given API_URL, MERCHANT_ID, STORE_ID
         */
        fun initialise(
            apiUrl: String,
            merchantId: String,
            storeId: String,

            ){
            if (apiUrl.isEmpty()) {
                throw Error("API URL shouldn't be empty")
            }
            if (!apiUrl.endsWith('/')) {
                baseUrl = "$apiUrl/"
            }


            Constants.setConstants(merchantId, storeId)
            _initialise = true
        }
        /**
         * Call this method to set the authToken
         * authToken can be generated from surfboard API as in the Surfboard developer docs
         * @see <a href="https://developers.surfboardpayments.com/docs/api/auth?lang=GO#Create-Token">surfboard developer docs</a>
         */
        fun setToken(authToken: String) {
            println("initiated token")
            if (authToken.isEmpty()) {
                throw Error("auth token shouldn't be empty")
            }
            _authToken = authToken
        }
    }

    private val client: OkHttpClient = OkHttpClient()
    suspend fun <ResponseType> makeApiCall(routes: SurfRouteValue, body: String?): ResponseType {

        if (!_initialise) {
            throw Error("Initialise SurfClient before making api calls")
        }
        val url: String = baseUrl + routes.route

        val headersBuilder: Headers.Builder = Headers.Builder()
        if (_authToken.isNotEmpty()) {
            headersBuilder.add("Authorization", "Bearer $_authToken")
        } else {
            throw Error("auth token shouldn't be empty")
        }
        val headers: Headers = headersBuilder.add("Content-Type", "application/json")
            .add("MERCHANT-ID", Constants.merchantId).build()

        val requestBuilder = Request.Builder().headers(headers).url(url)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody: RequestBody = RequestBody.create(mediaType, body)


        val request = when (routes.requestType) {
            RequestType.GET -> requestBuilder.get().build()
            RequestType.POST -> requestBuilder.post(requestBody).build()
            RequestType.DELETE -> requestBuilder.delete().build()
        }

println("request ${request.url().toString()}")
        println(request.headers().toString())

        val response = client.newCall(request).execute()
        val responseBodyString = String(response.body().bytes())
        val responseBody = JSONObject(responseBodyString)
println(responseBodyString)
        return routes.response(responseBody.toString()) as ResponseType
    }




}