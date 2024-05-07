package com.surfboardpayments.pos_app.services

class Constants() {
    companion object{
        var merchantId: String =""
                var storeId: String = ""
        var checkoutXterminalId:String = ""
        var orderId = ""
        fun setConstants(merchantId:String, storeId:String){
            this.merchantId = merchantId
            this.storeId = storeId
        }
        fun setTerminalId(terminalId:String){
            this.checkoutXterminalId = terminalId
        }
    }

}