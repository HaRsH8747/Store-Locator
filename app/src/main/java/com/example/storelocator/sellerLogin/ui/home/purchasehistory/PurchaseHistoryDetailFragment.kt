package com.example.storelocator.sellerLogin.ui.home.purchasehistory

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.databinding.FragmentPurchaseHistoryDetailBinding
import com.example.storelocator.util.Constants.Companion.purchaseHistory
import java.text.DecimalFormat

class PurchaseHistoryDetailFragment : Fragment() {

    private lateinit var binding: FragmentPurchaseHistoryDetailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPurchaseHistoryDetailBinding.inflate(layoutInflater)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvProductName.text = purchaseHistory.productName
        binding.tvProductPrice.text = purchaseHistory.productPrice.toString()
        if (purchaseHistory.discount == 0){
            binding.tvDiscount.text = "${purchaseHistory.extraDiscount}%"
        }else{
            binding.tvDiscount.text = "${purchaseHistory.discount}% + ${purchaseHistory.extraDiscount}%"
        }
        binding.tvcDiscountPrice.text = "₹${calculateDiscount(purchaseHistory.productPrice, purchaseHistory.discount+ purchaseHistory.extraDiscount)}"
        if (purchaseHistory.extraOffer == "No Extra Offer"){
            binding.textView15.visibility = View.GONE
            binding.tvExtraOffer.visibility = View.GONE
        }else{
            binding.tvExtraOffer.text = purchaseHistory.extraOffer
        }
        binding.tvquantity.text = purchaseHistory.quantity.toString()
        binding.tvTotalPrice.text = (purchaseHistory.quantity.toDouble() * calculateDiscount(purchaseHistory.productPrice, purchaseHistory.discount+ purchaseHistory.extraDiscount)).toString()
        binding.tvPurchaseTime.text = purchaseHistory.purchaseTime
        binding.btnphOK.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun calculateDiscount(productPrice: Double, discount: Int): Double{
        val discountObtained = ((discount/100.00) * productPrice)
        return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
    }
}