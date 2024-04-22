package com.surfboardpayments.pos_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import androidx.core.view.get
import com.surfboardpayments.pos_app.databinding.ActivityMainBinding
import com.surfboardpayments.pos_app.models.CodeGenerated
import com.surfboardpayments.pos_app.models.OrderCreated
import com.surfboardpayments.pos_app.models.PaymentInitiated
import com.surfboardpayments.pos_app.models.request_models.InitiateTransaction
import com.surfboardpayments.pos_app.models.request_models.ItemAmount
import com.surfboardpayments.pos_app.models.request_models.OrderDetails
import com.surfboardpayments.pos_app.models.request_models.Tax
import com.surfboardpayments.pos_app.models.request_models.initiateTransactionJson
import com.surfboardpayments.pos_app.models.request_models.orderDetailJson
import com.surfboardpayments.pos_app.services.Constants
import com.surfboardpayments.pos_app.services.LocalStorage
import com.surfboardpayments.pos_app.services.POSViews
import com.surfboardpayments.pos_app.services.RouteMapClass
import com.surfboardpayments.pos_app.services.SurfClient
import com.surfboardpayments.pos_app.services.SurfRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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
            apiKey = "API_KEY",
            apiSecret = "API_SECRET",
            merchantId = "MERCHANT_ID",
            storeId = "STORE_ID",
            authToken = "AUTH_TOKEN"
        )
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
                        registrationCode = this.data?.registrationCode ?: ""
                        switchToCheckoutX("checkoutx://com.surfboard.checkoutx/register?appScheme=surfposapp&appNameSpace=com.surfboardpayments.pos_app&registrationCode=$registrationCode")
                    }
                }
            }
        }),

        POSViews.EnterAmount to OnClickListeners(onClick = {}),

        POSViews.CreateOrder to OnClickListeners(onClick = {

            CoroutineScope(Dispatchers.Main).launch {
                createOrder().await().run {
                    if (this.status == "SUCCESS") {
                        println("registration code ${this.data?.orderId}")
                        orderId = this.data?.orderId ?: ""

                        addView(POSViews.StartTransaction)
                        removeView(POSViews.EnterAmount)
                        removeView(POSViews.CreateOrder)

                    }
                }
            }
        }),

        POSViews.StartTransaction to OnClickListeners(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                startTransaction().await().run {
                    if (this.status == "SUCCESS") {
                        println("registration code ${this.data?.paymentId}")
                        paymentId = this.data?.paymentId ?: ""

                        val transactionDLUrl: String =
                            "checkoutx://com.surfboard.checkoutx/order?appScheme=surfposapp&appNameSpace=com.surfboardpayments.pos_app&showReceipt=true"
                        switchToCheckoutX(transactionDLUrl)
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
        return CoroutineScope(Dispatchers.IO).async {
            return@async surfClient.makeApiCall<OrderCreated>(
                RouteMapClass().routeMap[SurfRoute.CreateOrder]!!, orderDetailJson(
                    OrderDetails(
                        Constants.checkoutXterminalId, "purchase", arrayListOf(), ItemAmount(
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

    private fun startTransaction(): Deferred<PaymentInitiated> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async surfClient.makeApiCall<PaymentInitiated>(
                RouteMapClass().routeMap[SurfRoute.InitiatePayment]!!, initiateTransactionJson(
                    InitiateTransaction(orderId, "CARD")
                )
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
                if (!uri.getQueryParameter("terminalId").isNullOrBlank()) {
                    Constants.setTerminalId(uri.getQueryParameter("terminalId")!!)
                    localStorage.store(uri.getQueryParameter("terminalId")!!)
                    handleViewAfterReg()
                }
                if (!uri.getQueryParameter("transactionId").isNullOrBlank()) {
                    handleViewAfterTransaction()
                }
            }
        }
    }


    private fun handleViewAfterReg() {
        removeView(POSViews.RegisterTerminal)
        addView(POSViews.EnterAmount)
        addView(POSViews.CreateOrder)
    }

    private fun handleViewAfterTransaction() {
        removeView(POSViews.StartTransaction)
        addView(POSViews.EnterAmount)
        addView(POSViews.CreateOrder)
    }
}

