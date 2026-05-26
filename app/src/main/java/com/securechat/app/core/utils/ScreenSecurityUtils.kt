package com.securechat.app.core.utils

import android.view.WindowManager
import android.app.Activity

object ScreenSecurityUtils {
    fun enableScreenshotProtection(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun disableScreenshotProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
