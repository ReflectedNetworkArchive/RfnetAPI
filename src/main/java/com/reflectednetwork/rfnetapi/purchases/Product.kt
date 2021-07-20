package com.reflectednetwork.rfnetapi.purchases

data class Product (
    val name: String,
    val price: Int,
    val oneTimePurchase: Boolean
)