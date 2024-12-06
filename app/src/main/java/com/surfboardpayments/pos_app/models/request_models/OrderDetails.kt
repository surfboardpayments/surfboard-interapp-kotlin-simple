package com.surfboardpayments.pos_app.models.request_models

import com.google.gson.annotations.SerializedName
import com.surfboardpayments.pos_app.services.serializer


data class OrderDetails(
    @SerializedName("terminal\$id")
    var terminalId: String,
    var type: String,
    var orderLines: ArrayList<LineItem>,
    var totalOrderAmount: TotalOrderAmount?,
    var purchaseOrderId: String=""

)

fun orderDetailJson(orderDetails: OrderDetails): String{
    return serializer.toJson(orderDetails,OrderDetails::class.java)
}

data class LineItem ( val id: String, val categoryId:String?="", val name:String, var quantity:Int, var itemAmount: ItemAmount


){
    fun copyWith(id:String?,categoryId: String?,name: String?,quantity: Int?,itemAmount: ItemAmount? ):LineItem{
        return LineItem(
            id=id?:this.id,
            categoryId = categoryId?:this.categoryId,
            name =name?:this.name,
            quantity = quantity?:this.quantity,
            itemAmount =itemAmount?:this.itemAmount

        )
    }
}
