
package com.petpals.shared.src.platform

import android.content.Context

object SharedAndroid {
    @Volatile private var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }
    fun context(): Context = requireNotNull(appContext) { "SharedAndroid.init(context) must be called from your Application" }
}
