package com.example.mahubephamarcy

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources.getDrawable

class create_account {
    fun openCreateAccount(view: View) {

        val FullnameEditText = view.findViewById<EditText>(R.id.fullname)
        val profileDrawable: Drawable = getDrawable(this,R.drawable.ic_profile)!!



    }
}