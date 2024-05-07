package com.surfboardpayments.pos_app.models

import com.surfboardpayments.pos_app.services.serializer

data class NoData(val status:String,val message:String)

fun noDataResponse(source:String):NoData{

    return serializer.fromJson(source,NoData::class.java)
}
