package com.example.storelocator.mainActivity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storelocator.login_register.register.customer.Customer
import com.example.storelocator.login_register.register.seller.two.Seller

class MainActivityViewModel: ViewModel() {

    private var _seller = MutableLiveData<Seller>()
    private var _customer = MutableLiveData<Customer>()

    val seller: LiveData<Seller> = _seller
    val customer: LiveData<Customer> = _customer

    fun saveSeller(newSeller: Seller){
        _seller.value = newSeller
    }

    fun saveCustomer(newCustomer: Customer){
        _customer.value = newCustomer
        Log.i("CLEAR","saveCustomer _customer: ${_customer.value}")
    }

    @JvmName("getCustomer1")
    fun getCustomer(): LiveData<Customer> {
        Log.i("CLEAR","getCustomer _customer: ${_customer.value}")
        return _customer
    }

}