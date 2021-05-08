package com.example.storelocator.customerLogin.qrCodeFiles

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.databinding.FragmentQRScannerBinding
import com.example.storelocator.util.Constants
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore


class QRScannerFragment : Fragment() {

    private lateinit var binding: FragmentQRScannerBinding
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var progressDialog: Dialog
    private lateinit var firestore: FirebaseFirestore

    @SuppressLint("InflateParams")
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentQRScannerBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        previewView = binding.cameraPreview
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_login, null)
            setContentView(inflate)
            val textView = findViewById<TextView>(R.id.pdTextView)
            textView.text = ""
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProvider()

        return binding.root
    }

    private fun cameraProvider() {
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startCamera(cameraProvider: ProcessCameraProvider?) {
        try {
            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

            val preview = Preview.Builder().apply {
                setTargetResolution(Size(previewView.width, previewView.height))
            }.build()

            val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(previewView.width, previewView.height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), QRCodeImageAnalyzer(object : QRCodeFoundListener {
                            override fun onQRCodeFound(qrCode: String) {
                                Constants.scannedQRCode = qrCode
                                findNavController().popBackStack()
                            }

                            override fun qrCodeNotFound() {

                            }
                        }))
                    }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }catch (e: Exception){
            Log.i("CLEAR", "QR: ${e.message}")
        }
    }
}
