package com.surfboardpayments.pos_app.models

import com.surfboardpayments.pos_app.services.serializer

data class PaymentInitiated (val status:String, val message:String, val data:PaymentData?)

data class PaymentData(
    val paymentId:String
)

fun paymentInitiated(source:String):PaymentInitiated{
    return serializer.fromJson(source,PaymentInitiated::class.java)
}
