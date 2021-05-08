package com.example.storelocator.login_register.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.storelocator.FireBaseUserLiveData

class LoginViewModel(): ViewModel() {


    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    var authenticationState = FireBaseUserLiveData().map { user ->
        Log.i("CLEAR", "User: ${user.toString()}")
        if (user != null){
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}