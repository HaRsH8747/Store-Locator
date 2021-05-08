package com.example.storelocator.customerLogin.home

import com.google.android.gms.maps.model.LatLng

data class StoreInfo(
    val storeName: String,
    val latLng: LatLng,
    val openingTime: String,
    val closingTime: String
)