package com.example.storelocator.customerLogin.productList

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentCustomerProductDetailBinding
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.PERMISSION_REQUEST_CAMERA
import com.example.storelocator.util.Constants.Companion.allDetailProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class CustomerProductDetailFragment : Fragment() {

    private lateinit var binding: FragmentCustomerProductDetailBinding
    private lateinit var progressDialog: Dialog
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var dialog: Dialog
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentCustomerProductDetailBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = SplashActivity.firebaseAuth
        dialog = Dialog(requireContext())
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_login, null)
            setContentView(inflate)
            val textView = findViewById<TextView>(R.id.pdTextView)
            textView.text = "Checking QR Code"
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Glide.with(requireActivity()).load(allDetailProduct.productImage).into(binding.ivcProductImage)
        if (allDetailProduct.discount == 0){
            binding.tvcDiscount.text = "(${allDetailProduct.extraDiscount}%)"
        }else{
            binding.tvcDiscount.text = "(${allDetailProduct.discount}% + ${allDetailProduct.extraDiscount}%)"
        }
        binding.tvcProductName.text = allDetailProduct.productName
        binding.tvcOriginalPrice.text = "₹${allDetailProduct.productPrice}"
        val discountPrice = calculateDiscount(allDetailProduct.productPrice, allDetailProduct.discount+ allDetailProduct.extraDiscount)
        binding.tvcDiscountPrice.text = "₹$discountPrice"
        binding.tvcExtraOffer.text = allDetailProduct.extraOffer
        binding.tvcStoreName.text = allDetailProduct.storeName
        binding.tvcStoreAddress.text = allDetailProduct.storeAddress
        binding.tvcStoreTiming.text = "${allDetailProduct.openingTime} - ${allDetailProduct.closingTime}"
        binding.scanNote.text = "(Note:- Scan the QR code to redeem extra ${allDetailProduct.extraDiscount}% discount on this product, You can get the QR Code from store owner)"
        mHandler = Handler(Looper.getMainLooper())
        binding.btnPlusQuantity.apply {
            isLongClickable = true
            setOnClickListener {
                val newQuantity = binding.etQuantity.text.toString().toInt() + 1
                binding.etQuantity.setText((newQuantity).toString())
                val newPrice = discountPrice * newQuantity
                binding.tvcDiscountPrice.text = "₹$newPrice"
            }
//            setOnLongClickListener {
//                mRunnable = Runnable {
//                    if (it.isPressed){
//                        binding.etQuantity.setText((binding.etQuantity.text.toString().toInt() + 1).toString())
//                    }
//                    mHandler.postDelayed(
//                            mRunnable, // Runnable
//                            500 // Delay in milliseconds
//                    )
//                }
//                mHandler.postDelayed(
//                        mRunnable, // Runnable
//                        1000 // Delay in milliseconds
//                )
//                false
//            }
        }

        binding.btnMinusQuantity.apply {
            isLongClickable = true
            setOnClickListener {
                if (binding.etQuantity.text.toString().toInt()-1>0){
                    val newQuantity = binding.etQuantity.text.toString().toInt() - 1
                    binding.etQuantity.setText((newQuantity).toString())
                    val newPrice = discountPrice * newQuantity
                    binding.tvcDiscountPrice.text = "₹$newPrice"
                }
            }
//            setOnLongClickListener {
//                mRunnable = Runnable {
//                    if (it.isPressed){
//                        if (binding.etQuantity.text.toString().toInt()-1>0){
//                            binding.etQuantity.setText((binding.etQuantity.text.toString().toInt() - 1).toString())
//                        }
//                    }
//                    mHandler.postDelayed(
//                            mRunnable, // Runnable
//                            500 // Delay in milliseconds
//                    )
//                }
//                mHandler.postDelayed(
//                        mRunnable, // Runnable
//                        1000 // Delay in milliseconds
//                )
//                false
//            }
        }
        binding.ivcQRScanner.setOnClickListener {
            requestCamera()
        }
        binding.btnLocateStore.setOnClickListener {
            val latLng = allDetailProduct.storeLatLng
            val gmmIntentUri = Uri.parse("google.navigation:q=${latLng.latitude},${latLng.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            mapIntent.resolveActivity(requireContext().packageManager)?.let {
                startActivity(mapIntent)
            }
        }
        binding.btnOk.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun requestCamera() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            findNavController().navigate(CustomerProductDetailFragmentDirections.actionCustomerProductDetailFragmentToQRScannerFragment())
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findNavController().navigate(CustomerProductDetailFragmentDirections.actionCustomerProductDetailFragmentToQRScannerFragment())
            } else {
                Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateDiscount(productPrice: Double, discount: Int): Double{
        val discountObtained = ((discount/100.00) * productPrice)
        return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
    }

    override fun onResume() {
        super.onResume()
        if (Constants.scannedQRCode.isNotEmpty()){
            checkQRCode(Constants.scannedQRCode)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkQRCode(code: String){
        try {
            progressDialog.show()
            val fireStoreRef3 = firestore.collection("sellers").document(code).collection("stores")
                    .document(allDetailProduct.storeName).collection("products").document(allDetailProduct.productName)
            Log.i("CLEAR", "ref: ${fireStoreRef3.path}")
            fireStoreRef3.get().addOnSuccessListener { document ->
                if (document.exists()){
                    progressDialog.dismiss()
                    dialog.setContentView(R.layout.dialog_confirm_purchase)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    val buttonYes = dialog.findViewById<Button>(R.id.btnConfirmYes)
                    val buttonNo = dialog.findViewById<Button>(R.id.btnConfirmNo)
                    dialog.show()
                    buttonNo.setOnClickListener {
                        dialog.dismiss()
                        Constants.scannedQRCode = ""
                    }
                    buttonYes.setOnClickListener {
                        dialog.dismiss()
                        Log.i("CLEAR", "yes")
                        val textView1 = progressDialog.findViewById<TextView>(R.id.pdTextView)
                        textView1.text = "Purchasing..."
                        progressDialog.show()
                        val popularity = (document.getDouble("popularity") as Double).toInt()
                        Log.i("CLEAR", "popularity $popularity")
                        val localDateTime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        val currentDateTime = localDateTime.format(formatter)
                        val customerPurchaseHistory = hashMapOf(
                                "productName" to allDetailProduct.productName,
                                "productPrice" to allDetailProduct.productPrice,
                                "quantity" to binding.etQuantity.text.toString().toInt(),
                                "discount" to allDetailProduct.discount,
                                "extraDiscount" to allDetailProduct.extraDiscount,
                                "extraOffer" to allDetailProduct.extraOffer,
                                "purchaseTime" to currentDateTime,
                                "storeName" to allDetailProduct.storeName,
                                "storeAddress" to allDetailProduct.storeAddress
                        )
                        val fireStoreRef1 = firestore.collection("customers").document(firebaseAuth.currentUser!!.uid)
                            .collection("purchaseHistory").document(currentDateTime)
                        fireStoreRef1.set(customerPurchaseHistory).addOnSuccessListener {

                            val sellerPurchaseHistory = hashMapOf(
                                    "productName" to allDetailProduct.productName,
                                    "productPrice" to allDetailProduct.productPrice,
                                    "quantity" to binding.etQuantity.text.toString().toInt(),
                                    "discount" to allDetailProduct.discount,
                                    "extraDiscount" to allDetailProduct.extraDiscount,
                                    "extraOffer" to allDetailProduct.extraOffer,
                                    "purchaseTime" to currentDateTime
                            )

                            val fireStoreRef2 = firestore.collection("sellers").document(Constants.scannedQRCode)
                                .collection("stores").document(allDetailProduct.storeName)
                                .collection("purchaseHistory").document(currentDateTime)
                            fireStoreRef2.set(sellerPurchaseHistory).addOnSuccessListener {

                                fireStoreRef3.update("popularity", popularity + 1).addOnSuccessListener {
                                    Log.i("CLEAR", "updated ${popularity + 1}")
                                    scanSuccessfulDialog()
                                }.addOnFailureListener {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        fireStoreRef1.delete().await()
                                        fireStoreRef2.delete().await()
                                        withContext(Dispatchers.Main){
                                            networkErrorDialog()
                                        }
                                    }
                                }
                            }.addOnFailureListener {
                                CoroutineScope(Dispatchers.IO).launch{
                                    fireStoreRef1.delete().await()
                                    withContext(Dispatchers.Main){
                                        networkErrorDialog()
                                    }
                                }
                            }
                        }.addOnFailureListener {
                            networkErrorDialog()
                        }
                    }
                }else{
                    invalidQRDialog()
                }
            }.addOnFailureListener {
                networkErrorDialog()
            }
        }catch (e: IllegalArgumentException){
            invalidQRDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun networkErrorDialog(){
        Constants.scannedQRCode = ""
        progressDialog.dismiss()
        dialog.setContentView(R.layout.dialog_invalid_qrcode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val button = dialog.findViewById<Button>(R.id.btnInvalidOk)
        val textView2 = dialog.findViewById<TextView>(R.id.tvInvalidQR)
        textView2.text = "Network Error!\nPlease try again"
        button.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun invalidQRDialog(){
        Constants.scannedQRCode = ""
        progressDialog.dismiss()
        dialog.setContentView(R.layout.dialog_invalid_qrcode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val button = dialog.findViewById<Button>(R.id.btnInvalidOk)
        button.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun scanSuccessfulDialog(){
        Constants.scannedQRCode = ""
        progressDialog.dismiss()
        dialog.setContentView(R.layout.dialog_scan_successful)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val button = dialog.findViewById<Button>(R.id.btnScanOk)
        button.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}