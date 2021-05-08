package com.example.storelocator.sellerLogin.ui.home.editproduct

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.storelocator.R
import com.example.storelocator.sellerLogin.ui.productslist.Product
import com.example.storelocator.sellerLogin.ui.productslist.SellerProductListFragmentDirections
import com.example.storelocator.util.Constants
import java.text.DecimalFormat

class SelectProductAdapter(
    private var products: List<Product>
): RecyclerView.Adapter<SelectProductAdapter.SelectProductViewHolder>() {

    inner class SelectProductViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        fun calculateDiscount(productPrice: Double, discount: Int): Double{
            val discountObtained = ((discount/100.00) * productPrice)
            return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.edit_product_select_item,parent,false)
        return SelectProductViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SelectProductViewHolder, position: Int) {
        holder.itemView.apply {
            val productName = findViewById<TextView>(R.id.tvEditProductName)
            val discountPrice = findViewById<TextView>(R.id.tvEditDiscountPrice)
            val productImage = findViewById<ImageView>(R.id.ivEditProduct)
            val product = products[position]

            Glide.with(context).load(product.productImage).into(productImage)
            productName.text = product.productName
            if (product.discount.toString().isBlank() || product.discount == 0){
                discountPrice.text = "₹${product.productPrice}"
            }else{
                discountPrice.text = "₹${holder.calculateDiscount(product.productPrice, product.discount+product.extraDiscount)}"
            }
        }

        holder.itemView.setOnClickListener {
            Constants.product = products[position]
            Log.i("CLEAR","${it.findNavController().currentDestination}")
            if (it.findNavController().currentDestination?.id == R.id.seller_navigation_dashboard){
                it.findNavController().navigate(SellerProductListFragmentDirections.actionSellerNavigationDashboardToProductDetailFragment())
            }
            if (it.findNavController().currentDestination?.id == R.id.selectProductFragment){
                it.findNavController().popBackStack()
            }
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }
}