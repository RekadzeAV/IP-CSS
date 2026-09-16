package com.company.ipcamera.android.media

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi

/**
 * Менеджер для управления Picture-in-Picture режимом
 */
class PictureInPictureManager(private val activity: Activity) {

    /**
     * Проверить, поддерживается ли PiP
     */
    fun isPictureInPictureSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }

    /**
     * Войти в режим Picture-in-Picture
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPictureInPictureMode(
        aspectRatio: Rational = Rational(16, 9)
    ): Boolean {
        if (!isPictureInPictureSupported()) {
            return false
        }

        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверить, находится ли активность в режиме PiP
     */
    fun isInPictureInPictureMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
    }
}
