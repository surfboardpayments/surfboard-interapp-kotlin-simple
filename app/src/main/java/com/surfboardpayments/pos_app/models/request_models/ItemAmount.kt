package com.surfboardpayments.pos_app.models.request_models

data class ItemAmount(val regular:Int, val campaign:Int?=0, val shipping:Int?=0, val total:Int, val currency: String, val tax: ArrayList<Tax> )
data class TotalOrderAmount(val regular:Int, val campaign:Int?=0, val shipping:Int?=0, val total:Int, val currency: String, val tax: ArrayList<Tax> )

