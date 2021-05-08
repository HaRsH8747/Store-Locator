package com.example.storelocator.sellerLogin.ui.home.editproduct

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentSelectProductBinding
import com.example.storelocator.sellerLogin.ui.productslist.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SelectProductFragment : Fragment() {

    private lateinit var binding: FragmentSelectProductBinding
    private var productList = mutableListOf<Product>()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSelectProductBinding.inflate(layoutInflater)
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)

        fetchProductsFromFireStore()

        return binding.root
    }

    private fun fetchProductsFromFireStore(){
        val userID = SplashActivity.firebaseAuth.currentUser?.uid.toString()
        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        val storageRef = storage.reference
        CoroutineScope(Dispatchers.IO).launch {
            val querySnapshot = firestore.collection("sellers").document(userID)
                .collection("stores").document(storeName.toString()).collection("products").orderBy("productName",
                    Query.Direction.ASCENDING).get().await()
                    for (productDocument in querySnapshot.documents){
                        val imagesUrl = storageRef.child("seller/$userID/$storeName/products/${productDocument.getString("productName") as String}.jpg").downloadUrl.await()
                        val product = Product(
                            productDocument.getString("productName") as String,
                            (productDocument.get("productPrice") as Double),
                            (productDocument.get("discount") as Long).toInt(),
                            (productDocument.get("extraDiscount") as Long).toInt(),
                            productDocument.getString("extraOffer") as String,
                            imagesUrl.toString(),
                            (productDocument.get("popularity") as Long).toInt(),
                        )
                        productList.add(product)
                    }
            withContext(Dispatchers.Main){
                binding.linearProgressBar.hide()
                val adapter = SelectProductAdapter(productList)
                binding.recyclerview.adapter = adapter
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestore.clearPersistence()
    }
}