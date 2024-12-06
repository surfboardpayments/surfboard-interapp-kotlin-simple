package com.surfboardpayments.pos_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import androidx.core.view.get
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.surfboardpayments.pos_app.databinding.ActivityMainBinding
import com.surfboardpayments.pos_app.models.CodeGenerated
import com.surfboardpayments.pos_app.models.NoData
import com.surfboardpayments.pos_app.models.OrderCreated
import com.surfboardpayments.pos_app.models.PaymentInitiated
import com.surfboardpayments.pos_app.models.request_models.InitiateTransaction
import com.surfboardpayments.pos_app.models.request_models.ItemAmount
import com.surfboardpayments.pos_app.models.request_models.LineItem
import com.surfboardpayments.pos_app.models.request_models.OrderDetails
import com.surfboardpayments.pos_app.models.request_models.Tax
import com.surfboardpayments.pos_app.models.request_models.TotalOrderAmount
import com.surfboardpayments.pos_app.models.request_models.initiateTransactionJson
import com.surfboardpayments.pos_app.models.request_models.orderDetailJson
import com.surfboardpayments.pos_app.services.Constants
import com.surfboardpayments.pos_app.services.LocalStorage
import com.surfboardpayments.pos_app.services.POSViews
import com.surfboardpayments.pos_app.services.RouteMapClass
import com.surfboardpayments.pos_app.services.SurfClient
import com.surfboardpayments.pos_app.services.SurfRoute
import com.surfboardpayments.pos_app.services.serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var _dynamicViewsItems: DynamicViews
    private var surfClient: SurfClient = SurfClient()
    private var registrationCode: String = ""
    private var orderId: String = ""
    private var paymentId: String = ""
    private lateinit var localStorage: LocalStorage

    init {

        SurfClient.initialise(
            apiUrl = "API_URL",
            merchantId = "MERCHANT_ID",
            storeId = "STORE_ID"
        )
        SurfClient.setToken(authToken = "AUTH_TOKEN")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        initialiseView()


    }

    private var callBackMap: Map<POSViews, OnClickListeners> = mapOf(

        POSViews.RegisterTerminal to OnClickListeners(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                registerTerminal().await().run {
                    if (this.status == "SUCCESS") {
                        println("registration code ${this.data?.registrationCode}")
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "Generated registration code"
                        )
                        registrationCode = this.data?.registrationCode!!
                        val terminalName = "surf${SecureRandom().nextInt(999)}"
                        val dataObject = JsonObject()
                        dataObject.addProperty("registrationCode", registrationCode)
                        dataObject.addProperty("terminalName", terminalName)
                        val dataAsBase64 = base64UrlEncoder(dataObject.toString())
                        //change host to com.surfboard.checkout_tester to support surfboard tester app.
                        val registerDLUrl =
                            "checkoutx://com.surfboard.checkoutx/register?redirectUrl=${returnRedirectUrl()}&data=$dataAsBase64"
                        switchToCheckoutX(registerDLUrl)
                    } else {
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "code generation failed with error ${this.message}"
                        )
                    }
                }
            }
        }),

        POSViews.EnterAmount to OnClickListeners(onClick = {}),

        POSViews.CreateOrder to OnClickListeners(onClick = {

            CoroutineScope(Dispatchers.Main).launch {
                createOrder().await().run {
                    if (this.status == "SUCCESS") {
                        println("orderId ${this.data?.orderId}")
                        orderId = this.data?.orderId!!
                        Constants.orderId = orderId
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "order created successfully with id $orderId"
                        )
                        addView(POSViews.StartTransaction)
                        addView(POSViews.CancelOrder)
                        removeView(POSViews.EnterAmount)
                        removeView(POSViews.CreateOrder)

                    } else {
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "Unable to create order with error ${this.message}"
                        )
                    }
                }
            }
        }),

        POSViews.StartTransaction to OnClickListeners(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                startTransaction().await().run {
                    if (this.status == "SUCCESS") {
                        println("PaymentId ${this.data?.paymentId}")
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "initiated payment with id $paymentId"
                        )
                        paymentId = this.data?.paymentId!!
                        val dataObject = JsonObject()

                        dataObject.addProperty("terminalId", Constants.checkoutXterminalId)
                        dataObject.addProperty("showReceipt", "true")
                        val dataAsBase64 = base64UrlEncoder(dataObject.toString())
                        //change host to com.surfboard.checkout_tester to support surfboard tester app.
                        val transactionDLUrl: String =
                            "checkoutx://com.surfboard.checkoutx/transaction?data=$dataAsBase64&redirectUrl=${returnRedirectUrl()}"
                        switchToCheckoutX(transactionDLUrl)
                    } else {
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            "Unable to initiate transaction ${this.message}"
                        )
                    }
                }
            }

        }


        ),
        POSViews.CancelOrder to OnClickListeners(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                cancelOrder().await().run {
                    if (this.status == "SUCCESS") {

                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            this.message
                        )

                        removeView(POSViews.CancelOrder)
                        removeView(POSViews.StartTransaction)
                        addView(POSViews.EnterAmount)
                        addView(POSViews.CreateOrder)
                    } else {
                        makeSnack(
                            findViewById<LinearLayout>(binding.dynamicView.id),
                            this.message
                        )
                    }
                }
            }


        }),


        )

    private fun initialiseView() {
        localStorage = LocalStorage(this)
        val view = findViewById<LinearLayout>(binding.dynamicView.id)
        _dynamicViewsItems = DynamicViews(view, applicationContext)
        val terminalId = localStorage.read()
        if (terminalId.isNotBlank()) {
            Constants.setTerminalId(terminalId)
            addView(POSViews.EnterAmount)
            addView(POSViews.CreateOrder)
        } else {
            addView(POSViews.RegisterTerminal)
        }

    }

    private fun addView(pv: POSViews) {

        _dynamicViewsItems.addView(pv.name, pv.viewName, pv.viewId, callBackMap[pv]!!)
    }


    private fun removeView(pv: POSViews) {
        _dynamicViewsItems.removeView(pv.name)
    }

    private fun registerTerminal(): Deferred<CodeGenerated> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async surfClient.makeApiCall<CodeGenerated>(
                RouteMapClass().routeMap[SurfRoute.GenerateRegistrationCode]!!, ""
            )
        }
    }

    private fun createOrder(): Deferred<OrderCreated> {
        val view = binding.dynamicView.get(0) as EditText
       val  value = view.text.toString().toIntOrNull() ?: 0;
        if (value==0){
            makeSnack(
                findViewById<LinearLayout>(binding.dynamicView.id),
                "Order amount should be greater than 0"
            )
        }else{
            return CoroutineScope(Dispatchers.IO).async {
                val itemAmount = ItemAmount(
                    (view.text.toString().toIntOrNull() ?: 0) * 100,
                    0,
                    0,
                    (view.text.toString().toIntOrNull() ?: 0) * 100,
                    "SEK",
                    arrayListOf(Tax(25, 25, "VAT"))
                )
                val orderLines = arrayListOf(
                    LineItem(
                        id = "itemId001",
                        name = "coffee",
                        quantity = 1,
                        itemAmount = itemAmount
                    )
                )
                return@async surfClient.makeApiCall<OrderCreated>(

                    RouteMapClass().routeMap[SurfRoute.CreateOrder]!!, orderDetailJson(
                        OrderDetails(
                            Constants.checkoutXterminalId,
                            "purchase",
                            orderLines = orderLines ,
                            totalOrderAmount = TotalOrderAmount(
                                (view.text.toString().toIntOrNull() ?: 0) * 100,
                                0,
                                0,
                                (view.text.toString().toIntOrNull() ?: 0) * 100,
                                "SEK",
                                arrayListOf(Tax(25, 25, "VAT"))
                            )
                        )
                    )
                )

            }
        }

    }

    private fun startTransaction(): Deferred<PaymentInitiated> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async surfClient.makeApiCall<PaymentInitiated>(
                RouteMapClass().routeMap[SurfRoute.InitiatePayment]!!, initiateTransactionJson(
                    InitiateTransaction(orderId, "CARD")
                )
            )

        }
    }

    private fun cancelOrder(): Deferred<NoData> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async surfClient.makeApiCall<NoData>(
                RouteMapClass().routeMap[SurfRoute.CancelOrder]!!, ""
            )

        }
    }

    private fun switchToCheckoutX(deepLinkUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(deepLinkUrl)
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle the deep link when a new intent is received
        intent?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(it: Intent) {
        if (it.action == Intent.ACTION_VIEW) {

            if (it.data != null) {
                val uri: Uri = it.data as Uri
                if (!uri.getQueryParameter("data").isNullOrBlank()) {

                    val data = String(Base64.getUrlDecoder().decode(uri.getQueryParameter("data")))
                    val jsonObject: JsonObject = serializer.fromJson(data, JsonObject::class.java)
                    makeSnack(
                        findViewById<LinearLayout>(binding.dynamicView.id),
                        jsonObject.get("message").asString
                    )
                    if (jsonObject.get("terminalId") != null && !jsonObject.get("terminalId").isJsonNull) {
                        val terminalId = jsonObject.get("terminalId").asString
                        if (terminalId.isNullOrBlank()) {
                            return
                        }
                        Constants.setTerminalId(terminalId)
                        localStorage.store(terminalId)
                        handleViewAfterReg()
                    }
                    if (jsonObject.get("transactionId") != null && !jsonObject.get("transactionId").isJsonNull) {
                        handleViewAfterTransaction()
                    }
                }
            }

        }
    }

    private fun returnRedirectUrl(): String {
        val url = "surfposapp://testcheckout/"
        return base64UrlEncoder(url)
    }

    private fun base64UrlEncoder(data: String): String {
        return Base64.getUrlEncoder().encodeToString(data.toByteArray())
    }

    private fun makeSnack(view: View, text: String) {
        val snackBar = Snackbar.make(
            view, text, Snackbar.LENGTH_LONG
        )

        snackBar.show()
    }

    private fun handleViewAfterReg() {
        removeView(POSViews.RegisterTerminal)
        addView(POSViews.EnterAmount)
        addView(POSViews.CreateOrder)
    }

    private fun handleViewAfterTransaction() {
        removeView(POSViews.CancelOrder)
        removeView(POSViews.StartTransaction)
        addView(POSViews.EnterAmount)
        addView(POSViews.CreateOrder)
    }
}

