package com.surfboardpayments.pos_app.models

import com.surfboardpayments.pos_app.services.serializer

data class OrderCreated(
    val status: String,
    val data: OrderData?,
    val message: String
)

data class OrderData(
    val orderId: String
)

fun orderCreated(source: String):OrderCreated{
    return serializer.fromJson(source,OrderCreated::class.java)
}
