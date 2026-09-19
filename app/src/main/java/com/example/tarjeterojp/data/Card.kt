package com.example.tarjeterojp.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Card(
    val name: String,
    val phone: String,
    val email: String,
    val dateReceived: Long = System.currentTimeMillis()
)
