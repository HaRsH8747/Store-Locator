package com.example.storelocator.customerLogin.productList

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.storelocator.R
import com.example.storelocator.customerLogin.home.CustomerHomeFragmentDirections
import com.example.storelocator.util.Constants
import java.text.DecimalFormat

class CustomerGridProductListAdapter(
    productsList: List<AllDetailProduct>
): RecyclerView.Adapter<CustomerGridProductListAdapter.CustomerGridProductListViewHolder>() {

    private var products: MutableList<AllDetailProduct> = mutableListOf()

    init {
        for (item in productsList){
            products.add(item)
        }
    }

    inner class CustomerGridProductListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun calculateDiscount(productPrice: Double, discount: Int): Double{
            val discountObtained = ((discount/100.00) * productPrice)
            return DecimalFormat(".##").format(productPrice - discountObtained).toDouble()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerGridProductListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.edit_product_select_item,parent,false)
        return CustomerGridProductListViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CustomerGridProductListViewHolder, position: Int) {
        holder.itemView.apply {
            val productName = findViewById<TextView>(R.id.tvEditProductName)
            val discountPrice = findViewById<TextView>(R.id.tvEditDiscountPrice)
            val productImage = findViewById<ImageView>(R.id.ivEditProduct)
            val product = products[position]

            Glide.with(context).load(product.productImage).into(productImage)

            if (product.discount.toString().isBlank() || product.discount == 0){
                productName.text = product.productName
                discountPrice.text = "₹${product.productPrice}"
            }else{
                productName.text = product.productName
                discountPrice.text = "₹${holder.calculateDiscount(product.productPrice, product.discount+product.extraDiscount)}"
            }
        }

        holder.itemView.setOnClickListener {
            Constants.allDetailProduct = products[position]
            if (it.findNavController().currentDestination?.id == R.id.customer_navigation_home){
                it.findNavController().navigate(CustomerHomeFragmentDirections.actionCustomerNavigationHomeToCustomerProductDetailFragment())
            }
            if (it.findNavController().currentDestination?.id == R.id.customer_navigation_product_list){
                it.findNavController().navigate(CustomerProductListFragmentDirections.actionCustomerNavigationProductListToCustomerProductDetailFragment())
            }
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    fun clear(){
        val size = products.size
        products.clear()
        notifyItemRangeRemoved(0,size)
    }
}