package com.example.storelocator.sellerLogin.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.databinding.FragmentQRCodeGeneratorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

class QRCodeGenerator : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentQRCodeGeneratorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQRCodeGeneratorBinding.inflate(layoutInflater)

        firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid.toString()
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(userId, BarcodeFormat.QR_CODE,600,600)
            val barcodeEncoder = BarcodeEncoder()
            val bitMap = barcodeEncoder.createBitmap(bitMatrix)
            binding.ivQRCode.setImageBitmap(bitMap)
        }catch (e: WriterException){
            Log.i("CLEAR",e.message.toString())
        }

        binding.btnOK.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }
}