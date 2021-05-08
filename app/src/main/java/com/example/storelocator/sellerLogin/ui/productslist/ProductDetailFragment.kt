package com.example.storelocator.sellerLogin.ui.productslist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.storelocator.databinding.FragmentProductDetailBinding
import com.example.storelocator.util.Constants.Companion.product
import java.text.DecimalFormat

class ProductDetailFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = FragmentProductDetailBinding.inflate(layoutInflater)


        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Glide.with(requireActivity()).load(product.productImage).into(binding.ivProductImage)
        binding.tvProductName.text = product.productName
        binding.tvProductPrice.text = "₹${product.productPrice}"
        if (product.discount == 0){
            binding.tvDiscount.text = "(${product.extraDiscount}%)"
        }else{
            binding.tvDiscount.text = "(${product.discount}% + ${product.extraDiscount}%)"
        }
        val discountPrice = calculateDiscount(product.productPrice, product.discount+ product.extraDiscount)
        binding.tvDiscountPrice.text = "₹$discountPrice"
        binding.tvExtraOffer.text = product.extraOffer
        binding.tvPopularity.text = product.popularity.toString()
        binding.btnOk.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun calculateDiscount(productPrice: Double, discount: Int): Double{
        val discountObtained = ((discount/100.00) * productPrice)
        return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
    }
}