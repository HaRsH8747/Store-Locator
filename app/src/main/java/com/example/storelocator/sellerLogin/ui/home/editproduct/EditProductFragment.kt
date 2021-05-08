package com.example.storelocator.sellerLogin.ui.home.editproduct

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentEditProductBinding
import com.example.storelocator.sellerLogin.ui.productslist.Product
import com.example.storelocator.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import java.io.ByteArrayOutputStream

class EditProductFragment : Fragment() {

    private lateinit var binding: FragmentEditProductBinding
    private lateinit var product: Product
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var uri: Uri = Uri.EMPTY
    private lateinit var progressDialog: Dialog
    var popularity = 0

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentEditProductBinding.inflate(layoutInflater)
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()


        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_add_product, null)
            setContentView(inflate)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.ivEditProductImage.setOnClickListener {
            CropImage.activity().setAspectRatio(1, 1).start(requireContext(), this)
        }

        binding.btnSelectProduct.setOnClickListener {
            findNavController().navigate(EditProductFragmentDirections.actionEditProductFragmentToSelectProductFragment())
        }

        binding.btnSave.setOnClickListener {
            if (validateFields()){
                editProduct()
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(EditProductFragmentDirections.actionEditProductFragmentToSellerNavigationHome())
        }

        return binding.root
    }

    private fun editProduct() {
        val userID = firebaseAuth.uid.toString()
        val productName = binding.etProductName.text.toString()
        val productPrice = binding.etProductPrice.text.toString().toDouble()
        val discount = if (binding.etAddDiscount.text.isEmpty()) 0 else binding.etAddDiscount.text.toString().toInt()
        val extraDiscount = if (binding.etAppDiscount.text.isEmpty()) 0 else binding.etAppDiscount.text.toString().toInt()
        val extraOffer = if(binding.etaddExtraOffer.text.isEmpty()) "" else binding.etaddExtraOffer.text.toString()
        val product = hashMapOf(
                "productName" to productName,
                "productPrice" to productPrice,
                "discount" to discount,
                "extraDiscount" to extraDiscount,
                "extraOffer" to extraOffer,
                "popularity" to popularity
        )

        val storageRef = storage.reference
        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        val imagesRef = storageRef.child("seller/$userID/${storeName}/products/$productName.jpg")
        val uploadTask: UploadTask = if (uri == Uri.EMPTY){
            binding.ivEditProductImage.setImageResource(R.drawable.ic_baseline_broken_image_24)
            val drawable = (binding.ivEditProductImage.drawable as VectorDrawable)
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0,0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            imagesRef.putBytes(data)
        }else{
            imagesRef.putFile(uri)
        }
        Log.i("CLEAR",uri.toString())
        progressDialog.show()
        uploadTask.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Unable to upload product Image", Toast.LENGTH_LONG).show()
            Log.i("CLEAR", "Error in uploading product image")
        }.addOnCompleteListener {
            firestore.collection("sellers").document(userID).collection("stores")
                    .document(storeName.toString()).collection("products").document(productName).set(product, SetOptions.merge())
                    .addOnCompleteListener {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(),"Product updated", Toast.LENGTH_LONG).show()
                        Constants.cacheProductsList.clear()
                        Log.i("CLEAR", "Product added with name $productName")
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Log.i("CLEAR", "Error adding document", e)
                    }
        }
    }

    private fun validateFields():Boolean{
        if (binding.etProductName.text.isEmpty() || binding.etProductPrice.text.isEmpty() || binding.etAppDiscount.text.isEmpty()){
            if (binding.etProductName.text.isEmpty()){
                binding.etProductName.error = "Enter Product Name"
            }
            if (binding.etProductPrice.text.isEmpty()){
                binding.etProductPrice.error = "Enter Product Price"
            }
            if (binding.etAddDiscount.text.isEmpty()){
                binding.etAddDiscount.error = "Enter discount on product"
            }
            if (binding.etaddExtraOffer.text.isEmpty()){
                binding.etaddExtraOffer.error = "Enter extra offers"
            }
            if (binding.etAppDiscount.text.isEmpty()){
                binding.etAppDiscount.error = "Add Extra discount for App users, It can be redeemed by scanning your QR code only"
            }
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.i("CLEAR","RESUMED")
        if (checkProductData()){
            binding.etProductName.apply {
                isFocusableInTouchMode = true
                backgroundTintList = null
                setHintTextColor(null)
                setText(product.productName)
            }
            binding.etProductPrice.apply {
                isFocusableInTouchMode = true
                backgroundTintList = null
                setHintTextColor(null)
                setText(product.productPrice.toString())
            }
            binding.etAddDiscount.apply {
                isFocusableInTouchMode = true
                backgroundTintList = null
                setHintTextColor(null)
                setText(product.discount.toString())
            }
            binding.etaddExtraOffer.apply {
                isFocusableInTouchMode = true
                backgroundTintList = null
                setHintTextColor(null)
                setText(product.extraOffer)
            }
            binding.etAppDiscount.apply {
                isFocusableInTouchMode = true
                backgroundTintList = null
                setHintTextColor(null)
                setText(product.extraDiscount.toString())
            }
            uri = product.productImage.toUri()
            popularity = product.popularity
            Glide.with(requireActivity()).load(product.productImage).into(binding.ivEditProductImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Constants.product = Product("",0.0,0,0,"","",0)
    }

    private fun checkProductData(): Boolean {
        product = Constants.product
        if (product.productName=="" && product.productPrice==0.0 && product.discount==0 && product.extraDiscount==0 && product.extraOffer==""){
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null){
            val result = CropImage.getActivityResult(data)
            uri = result.uri
            Log.i("CLEAR", "URI:$uri")
            Glide.with(binding.root)
                    .load(uri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(CircleCrop())
                    .into(binding.ivEditProductImage)
        }else{
            Log.i("CLEAR","Image crop error")
            Toast.makeText(requireContext(), "product image not loaded", Toast.LENGTH_LONG).show()
        }
    }
}