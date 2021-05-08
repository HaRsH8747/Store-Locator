package com.example.storelocator.customerLogin.productList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CustomerProductListViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is product list Fragment"
    }
    val text: LiveData<String> = _text
}