package com.surfboardpayments.pos_app

import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class OnClickListeners(val onClick: () -> Unit)
class DynamicViews constructor(
    private val linearLayout: LinearLayout,
    private val context: Context
) {
    private val dynamicViewItems: MutableMap<String, TextView> = mutableMapOf()
 private   fun addButton(
        name: String,
        buttonText: String,
        id: Int,
        clickListeners: OnClickListeners,
    ) {
        val button = Button(
            this.context
        )
        button.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        button.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            320F,
            context.resources.displayMetrics
        ).toInt()

        button.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            4F,
            context.resources.displayMetrics
        )

        button.text = buttonText
        button.id = id

        button.setOnClickListener { clickListeners.onClick() }
        linearLayout.addView(button)
        dynamicViewItems[name] = button
    }

    fun removeView(name: String) {
        val view = dynamicViewItems[name] ?: return
        linearLayout.removeView(view)
        dynamicViewItems.remove(name)
    }

 private fun addEditText( name: String,
                    hintText: String,
                    id: Int,){
     val textField =  EditText(this.context)
       textField.layoutParams = LinearLayout.LayoutParams(
           LinearLayout.LayoutParams.WRAP_CONTENT,
           LinearLayout.LayoutParams.WRAP_CONTENT
       )

       textField.width = TypedValue.applyDimension(
           TypedValue.COMPLEX_UNIT_DIP,
           320F,
           context.resources.displayMetrics
       ).toInt()

       textField.textSize = TypedValue.applyDimension(
           TypedValue.COMPLEX_UNIT_SP,
           4F,
           context.resources.displayMetrics
       )

       textField.hint = hintText
       textField.id = id
textField.inputType = InputType.TYPE_CLASS_NUMBER

       linearLayout.addView(textField)
       dynamicViewItems[name] = textField
   }


    fun addView( name: String,
                 viewText: String,
                 id: Int,
                 clickListeners: OnClickListeners,){
        when(name){
            "RegisterTerminal"->{
                addButton(name,viewText,id,clickListeners)
            }
            "StartTransaction"->{
                addButton(name,viewText,id,clickListeners)
            }
            "CreateOrder"->{
                addButton(name,viewText,id,clickListeners)
            }
            "EnterAmount"->{
                addEditText(name,viewText,id)
            }
        }
    }

}