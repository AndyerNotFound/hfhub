package com.hfhub.android.data

import android.content.Context
import android.content.SharedPreferences


object SettingsStore {

    private const val PREFS = "hfhub_settings"
    private const val KEY_BASE = "base_url"
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = sp.getString(KEY_BASE, HfApi.DEFAULT_BASE) ?: HfApi.DEFAULT_BASE
        set(v) = sp.edit().putString(KEY_BASE, v.trim().trimEnd('/')).apply()
}
