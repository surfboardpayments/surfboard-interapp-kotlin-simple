package com.surfboardpayments.pos_app.models

import com.surfboardpayments.pos_app.services.serializer

data class CodeGenerated(val status:String,val message:String,val data:Code?)

data class Code(
    val registrationCode: String
)
fun codeGenerated(source: String):CodeGenerated{
    return serializer.fromJson(source,CodeGenerated::class.java)
}