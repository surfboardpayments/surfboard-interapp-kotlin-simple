package com.surfboardpayments.pos_app.services

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class LocalStorage(activity: Activity) {
    private var sp: SharedPreferences? = null

    init {
        sp = activity.getSharedPreferences("checkoutXSharedPref", MODE_PRIVATE);
    }

    fun store(id: String) {
        val edit = sp?.edit()
        if (edit != null) {
            edit.putString("terminalId", id)
            edit.apply()
        }
    }

    fun read(): String {
        return sp?.getString("terminalId", "") ?: ""
    }

}