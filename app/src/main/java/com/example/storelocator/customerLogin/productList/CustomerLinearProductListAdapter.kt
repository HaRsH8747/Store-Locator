package com.example.storelocator.customerLogin.productList

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.storelocator.R
import com.example.storelocator.util.Constants
import java.text.DecimalFormat

open class CustomerLinearProductListAdapter(
    productsList: List<AllDetailProduct>
):RecyclerView.Adapter<CustomerLinearProductListAdapter.ProductListViewHolder>() {

    private var products: MutableList<AllDetailProduct> = mutableListOf()

    init {
        products.addAll(productsList)
    }

    inner class ProductListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun calculateDiscount(productPrice: Double, discount: Int): Double{
            val discountObtained = ((discount/100.00) * productPrice)
            return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.add_product_item,parent,false)

        return ProductListViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductListViewHolder, position: Int) {
        holder.itemView.apply {
            val productName = findViewById<TextView>(R.id.tvProductName)
            val productPrice = findViewById<TextView>(R.id.tvProductPrice)
            val discountPrice = findViewById<TextView>(R.id.tvDiscountPrice)
            val discount = findViewById<TextView>(R.id.tvDiscount)
            val extraOffer = findViewById<TextView>(R.id.tvExtraOffer)
            val productImage = findViewById<ImageView>(R.id.ivListProduct)
            val product = products[position]

            productPrice.visibility = View.VISIBLE
            discount.visibility = View.VISIBLE

            Glide.with(context).load(product.productImage).into(productImage)

            if (product.discount.toString().isBlank() || product.discount == 0){
                discount.text = "(${product.extraDiscount}%)"
            }else{
                discount.text = "(${product.discount}% + ${product.extraDiscount}%)"
            }
            productName.text = product.productName
            discountPrice.text = "₹${holder.calculateDiscount(product.productPrice, product.discount+product.extraDiscount)}"
            productPrice.text = "₹${product.productPrice}"
            productPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            extraOffer.text = product.extraOffer
        }

        holder.itemView.setOnClickListener {
            Constants.allDetailProduct = products[position]
            it.findNavController().navigate(CustomerProductListFragmentDirections.actionCustomerNavigationProductListToCustomerProductDetailFragment())
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }
}