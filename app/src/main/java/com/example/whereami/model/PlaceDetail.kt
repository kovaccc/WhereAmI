package com.example.whereami.model

data class PlaceDetail(
        val latitude: Double,
        val longitude: Double,
        val country: String,
        val city: String,
        val address: String
)