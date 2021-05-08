package com.example.storelocator.sellerLogin.ui.home.purchasehistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentPurchaseHistoryBinding
import com.example.storelocator.util.Constants.Companion.cachePurchaseHistoryList
import com.example.storelocator.util.Constants.Companion.job4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PurchaseHistoryFragment : Fragment() {

    private lateinit var binding: FragmentPurchaseHistoryBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var purchaseHistoryList = mutableListOf<PurchaseHistory>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = FragmentPurchaseHistoryBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (cachePurchaseHistoryList.isNotEmpty() && !binding.purchaseHistorySwipeRefresh.isRefreshing){
            binding.purchaseHistorySwipeRefresh.isRefreshing = false
            purchaseHistoryList.clear()
            purchaseHistoryList.addAll(cachePurchaseHistoryList)
            binding.phLinearProgressBar.hide()
            val purchaseHistoryAdapter = PurchaseHistoryAdapter(purchaseHistoryList)
            binding.phRecyclerview.layoutManager = LinearLayoutManager(requireContext())
            binding.phRecyclerview.adapter = purchaseHistoryAdapter
        }else{
            fetchPurchaseHistoryFromFireStore()
        }
    }

    private fun fetchPurchaseHistoryFromFireStore() {
        purchaseHistoryList.clear()
        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        job4 = CoroutineScope(Dispatchers.IO).launch {
            val fireStoreRef = firestore.collection("sellers").document(firebaseAuth.currentUser!!.uid)
                    .collection("stores").document(storeName!!)
                    .collection("purchaseHistory").get().await()

            for (phDocument in fireStoreRef.documents){
                val purchaseHistory = PurchaseHistory(
                        phDocument.getString("productName") as String,
                        phDocument.get("productPrice") as Double,
                        (phDocument.get("quantity") as Long).toInt(),
                        (phDocument.get("discount") as Long).toInt(),
                        (phDocument.get("extraDiscount") as Long).toInt(),
                        phDocument.getString("extraOffer") as String,
                        phDocument.getString("purchaseTime") as String
                )

                purchaseHistoryList.add(purchaseHistory)
            }

            if (job4.isActive){
                cachePurchaseHistoryList.clear()
                cachePurchaseHistoryList.addAll(purchaseHistoryList)

                withContext(Dispatchers.Main){
                    if (purchaseHistoryList.isEmpty()){
                        binding.tvphNoProduct.visibility = View.VISIBLE
                        binding.phLinearProgressBar.hide()
                        binding.purchaseHistorySwipeRefresh.isRefreshing = false
                    }else{
                        binding.phLinearProgressBar.hide()
                        val purchaseHistoryAdapter = PurchaseHistoryAdapter(purchaseHistoryList)
                        binding.phRecyclerview.layoutManager = LinearLayoutManager(requireContext())
                        binding.phRecyclerview.adapter = purchaseHistoryAdapter
                    }
                }
            }
        }
    }
}