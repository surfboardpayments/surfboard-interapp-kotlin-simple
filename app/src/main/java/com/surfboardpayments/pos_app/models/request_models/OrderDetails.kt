package com.surfboardpayments.pos_app.models.request_models

import com.google.gson.annotations.SerializedName
import com.surfboardpayments.pos_app.services.serializer


data class OrderDetails(
    @SerializedName("terminal\$id")
    var terminalId: String,
    var type: String,
    var orderLines: ArrayList<Any>,
    var totalOrderAmount: ItemAmount?,
    var purchaseOrderId: String=""

)

fun orderDetailJson(orderDetails: OrderDetails): String{
    return serializer.toJson(orderDetails,OrderDetails::class.java)
}
