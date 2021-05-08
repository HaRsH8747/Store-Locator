package com.example.storelocator.sellerLogin.ui.home.purchasehistory

data class PurchaseHistory(
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val discount: Int,
    val extraDiscount: Int,
    val extraOffer: String,
    val purchaseTime: String
)