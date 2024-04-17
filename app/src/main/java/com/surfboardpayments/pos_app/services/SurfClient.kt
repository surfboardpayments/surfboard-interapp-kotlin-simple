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
        private var _apiKey: String? = null
        private var _apiSecret: String? = null
        private var _authToken: String? = null
        /**
         * Use the given API_URL, MERCHANT_ID, STORE_ID, API_KEY and API_SECRET
         * Either API_KEY and API_SECRET or AUTH_TOKEN have to be given
         *
         * AUTH_TOKEN can be generated from surfboard API as in the Surfboard developer docs
         * @see <a href="https://developers.surfboardpayments.com/docs/api/auth?lang=GO#Create-Token">surfboard developer docs</a>
         */
        fun initialise(
            apiUrl: String,
            apiSecret: String?,
            apiKey: String?,
            merchantId: String,
            storeId: String,
            authToken: String?
        ){
            if ((apiKey == null && apiSecret == null) && authToken == null) {
                throw AssertionError("Either authToken or apiKey and apiSecret shouldn't be null")
            }
            if (apiUrl.isEmpty()) {
                throw Error("API URL shouldn't be empty")
            }
            if (!apiUrl.endsWith('/')) {
                baseUrl = "$apiUrl/"
            }

            _apiKey = apiKey
            _apiSecret = apiSecret
            _authToken = authToken
            Constants.setConstants(merchantId, storeId)
            _initialise = true
        }

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
        if (_authToken != null) {
            headersBuilder.add("Authorization", "Bearer $_authToken")
        } else {
            headersBuilder.add("API-KEY", _apiKey).add("API-SECRET", _apiSecret)
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


        val response = client.newCall(request).execute()
        val responseBodyString = String(response.body().bytes())
        val responseBody = JSONObject(responseBodyString)

        return routes.response(responseBody.toString()) as ResponseType
    }




}
