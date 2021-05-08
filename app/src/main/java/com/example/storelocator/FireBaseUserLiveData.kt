package com.example.storelocator

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FireBaseUserLiveData: LiveData<FirebaseUser?>() {

    companion object{
        val firebaseAuth = SplashActivity.firebaseAuth
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        value = firebaseAuth.currentUser
    }

    override fun onActive() {
        super.onActive()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        super.onInactive()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}