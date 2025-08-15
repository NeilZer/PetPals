
package com.petpals.shared.data.preferences

import platform.Foundation.NSUserDefaults

actual class UserPreferences {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String): String {
        val value = userDefaults.stringForKey(key)
        return value ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        val value = userDefaults.integerForKey(key)
        return if (userDefaults.objectForKey(key) != null) value.toInt() else defaultValue
    }

    actual fun setInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), key)
        userDefaults.synchronize()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun setBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        val value = userDefaults.integerForKey(key)
        return if (userDefaults.objectForKey(key) != null) value else defaultValue
    }

    actual fun setLong(key: String, value: Long) {
        userDefaults.setInteger(value, key)
        userDefaults.synchronize()
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        val value = userDefaults.floatForKey(key)
        return if (userDefaults.objectForKey(key) != null) value else defaultValue
    }

    actual fun setFloat(key: String, value: Float) {
        userDefaults.setFloat(value, key)
        userDefaults.synchronize()
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    actual fun clear() {
        val domain = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        if (domain != null) {
            userDefaults.removePersistentDomainForName(domain)
            userDefaults.synchronize()
        }
    }

    actual fun contains(key: String): Boolean {
        return userDefaults.objectForKey(key) != null
    }
}
