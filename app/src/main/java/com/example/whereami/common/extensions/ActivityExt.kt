package com.example.whereami.common.extensions

import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import es.dmoral.toasty.Toasty

fun Activity.hasPermissions(permissions: Array<String>): Boolean {
    if (permissions.isNotEmpty()) {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                            this,
                            permission
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
    }
    return true
}

fun Activity.toast(resId: Int) {
    Toasty.info(this, resId, Toast.LENGTH_SHORT, true).show()
}

