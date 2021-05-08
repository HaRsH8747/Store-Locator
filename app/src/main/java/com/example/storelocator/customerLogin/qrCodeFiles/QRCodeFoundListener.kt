package com.example.storelocator.customerLogin.qrCodeFiles

interface QRCodeFoundListener {
    fun onQRCodeFound(qrCode: String)
    fun qrCodeNotFound()
}