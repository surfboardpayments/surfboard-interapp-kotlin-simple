package com.surfboardpayments.pos_app.services

enum class POSViews(val viewId:Int, val viewName:String) {
    RegisterTerminal (0x001,"Register Terminal"),
    EnterAmount(0x002, "Enter Amount"),
    CreateOrder(0x003,"Create Order"),
    StartTransaction(0x004,"Start Transaction")

}