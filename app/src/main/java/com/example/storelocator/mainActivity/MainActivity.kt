package com.example.storelocator.mainActivity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.storelocator.R
import com.example.storelocator.SplashActivity.Companion.firebaseAuth
import com.example.storelocator.SplashActivity.Companion.sharedPreferences
import com.example.storelocator.util.Constants
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FindMyStore)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(Constants.MY_SHARED_PREF, MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        val navController = this.findNavController(R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        Log.i("CLEAR", "Navigated through arrow(Navigated UP)")
        return navController.navigateUp()
    }
}