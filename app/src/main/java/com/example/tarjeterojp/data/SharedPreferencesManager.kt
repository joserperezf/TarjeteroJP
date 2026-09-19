package com.example.tarjeterojp.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TarjeteroJP_Prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun saveProfile(profile: Profile) {
        val jsonAdapter = moshi.adapter(Profile::class.java)
        val json = jsonAdapter.toJson(profile)
        sharedPreferences.edit().putString("PROFILE", json).apply()
    }

    fun getProfile(): Profile? {
        val json = sharedPreferences.getString("PROFILE", null) ?: return null
        val jsonAdapter = moshi.adapter(Profile::class.java)
        return jsonAdapter.fromJson(json)
    }

    fun saveReceivedCards(cards: List<Card>) {
        val type = Types.newParameterizedType(List::class.java, Card::class.java)
        val jsonAdapter = moshi.adapter<List<Card>>(type)
        val json = jsonAdapter.toJson(cards)
        sharedPreferences.edit().putString("RECEIVED_CARDS", json).apply()
    }

    fun getReceivedCards(): List<Card> {
        val json = sharedPreferences.getString("RECEIVED_CARDS", null) ?: return emptyList()
        val type = Types.newParameterizedType(List::class.java, Card::class.java)
        val jsonAdapter = moshi.adapter<List<Card>>(type)
        return jsonAdapter.fromJson(json) ?: emptyList()
    }
}
