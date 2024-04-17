package com.surfboardpayments.pos_app.models.request_models

import com.surfboardpayments.pos_app.services.serializer

data class InitiateTransaction(val orderId:String, val paymentMethod:String)

fun initiateTransactionJson(initiatePayment: InitiateTransaction):String{
    return serializer.toJson(initiatePayment, InitiateTransaction::class.java)
}

