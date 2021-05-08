package com.example.storelocator.sellerLogin.ui.productslist

data class Product(
        val productName: String,
        val productPrice: Double,
        val discount: Int,
        val extraDiscount: Int,
        val extraOffer: String,
        var productImage: String,
        val popularity: Int
)