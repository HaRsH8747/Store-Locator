package com.example.storelocator.customerLogin.productList

import com.google.android.gms.maps.model.LatLng

data class AllDetailProduct(
        val storeName: String,
        val storeAddress: String,
        val storeLatLng: LatLng,
        val openingTime: String,
        val closingTime: String,
        val productName: String,
        val productPrice: Double,
        val discount: Int,
        val extraDiscount: Int,
        val extraOffer: String,
        val popularity: Int,
        var productImage: String
)