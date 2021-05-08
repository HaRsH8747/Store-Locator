package com.example.storelocator.sellerLogin.ui.home.purchasehistory

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.storelocator.R
import com.example.storelocator.util.Constants

class PurchaseHistoryAdapter(
    private var purchaseHistoryList: List<PurchaseHistory>
): RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseHistoryViewHolder>() {

    inner class PurchaseHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.purchase_history_item,parent,false)
        return PurchaseHistoryViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PurchaseHistoryViewHolder, position: Int) {
        holder.itemView.apply {
            val srNo = findViewById<TextView>(R.id.tvSrNo)
            val productName = findViewById<TextView>(R.id.tvphProductName)
            val quantity = findViewById<TextView>(R.id.tvphQuantity)
            val purchaseHistory = purchaseHistoryList[position]

            srNo.text = (position+1).toString()
            productName.text = purchaseHistory.productName
            quantity.text = "Qty: ${purchaseHistory.quantity}"
        }

        holder.itemView.setOnClickListener {
            Constants.purchaseHistory = purchaseHistoryList[position]
            it.findNavController().navigate(PurchaseHistoryFragmentDirections.actionPurchaseHistoryFragmentToPurchaseHistoryDetailFragment())
        }
    }

    override fun getItemCount(): Int {
        return purchaseHistoryList.size
    }
}