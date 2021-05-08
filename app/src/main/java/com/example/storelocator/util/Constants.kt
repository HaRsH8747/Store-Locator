package com.example.storelocator.util

import android.net.Uri
import com.example.storelocator.customerLogin.home.StoreInfo
import com.example.storelocator.customerLogin.productList.AllDetailProduct
import com.example.storelocator.sellerLogin.ui.home.purchasehistory.PurchaseHistory
import com.example.storelocator.sellerLogin.ui.productslist.Product
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job

open class Constants {

    companion object{
        var isLocated: Boolean = false
        var isVerified: Boolean = false
        var latLng = LatLng(0.0,0.0)
        const val MY_SHARED_PREF = "FindMyStore"
        const val ERROR_DIALOG_REQUEST = 9001
        const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002
        const val PERMISSIONS_REQUEST_ENABLE_GPS = 9003
        const val PERMISSION_REQUEST_CAMERA = 9004
        var QR_CODE_GENERATED_FIRST_TIME = false
        const val sellerFile = "SELLER_DATA"
        const val customerFile = "CUSTOMER_DATA"
        var uri: Uri = Uri.EMPTY
        var imageData: ByteArray = ByteArray(DEFAULT_BUFFER_SIZE)
        const val userType = "USER_TYPE"
//        var storeName = ""
        var isLinearView = true
        var isNearbyLinearView = true
        var scannedQRCode = ""
        var product = Product("",0.0,0,0,"","",0)
        var cacheProductsList = mutableListOf<Product>()
        var allDetailProduct = AllDetailProduct("","",LatLng(0.0,0.0),"","","",0.0,0,0,"",0,"")
        var cacheAllProductsList = mutableListOf<AllDetailProduct>()
        var cacheNearbyProductsList = mutableListOf<AllDetailProduct>()
        var cacheStoreLocationList = mutableListOf<StoreInfo>()
        var purchaseHistory = PurchaseHistory("",0.0,0,0,0,"","")
        var cachePurchaseHistoryList = mutableListOf<PurchaseHistory>()
        var cacheLoadingCounter = 0
        lateinit var job1: Job
        lateinit var job2: Job
        lateinit var job3: Job
        lateinit var job4: Job
        lateinit var job5: Job
        var checked = 0
    }
}