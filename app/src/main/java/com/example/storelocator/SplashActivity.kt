package com.example.storelocator

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.storelocator.customerLogin.CustomerNavigationActivity
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.sellerLogin.ui.SellerNavigationActivity
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.checked
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.*


@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {

    private lateinit var language: String

    companion object{
        lateinit var sharedPreferences: SharedPreferences
        lateinit var firebaseAuth: FirebaseAuth
        @SuppressLint("StaticFieldLeak")
        private lateinit var firestore: FirebaseFirestore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_splash)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences(Constants.MY_SHARED_PREF, MODE_PRIVATE)
        // This is used to hide the status bar and make
        // the splash screen as a full screen activity.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // we used the postDelayed(Runnable, time) method
        // to send a message with a delayed time.

        //checkUserType()
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
            if (checked==0){
                checkUserType()
                checked = 1
            }
        }, 1000) // 1000 is the delayed time in milliseconds.
    }

    private fun checkUserType() {
        try {
            if (firebaseAuth.currentUser != null) {

                if (firebaseAuth.currentUser!!.isEmailVerified){
                    Constants.isVerified = true
                }

                val fis = openFileInput(Constants.userType)
                val isr = InputStreamReader(fis)
                val br = BufferedReader(isr)
                when(br.readLine()){
                    "customer" -> {
                        Log.i("CLEAR","customer")
                        val intent = Intent(this@SplashActivity, CustomerNavigationActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    "seller" -> {
                        Log.i("CLEAR","seller")
                        val intent = Intent(this@SplashActivity, SellerNavigationActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    else -> {
                        Log.i("CLEAR","MAIN 1")
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            } else {
                Log.i("CLEAR","MAIN 2")
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }catch (e: FileNotFoundException){
            Log.i("CLEAR","File Not Found")
            Log.i("CLEAR","MAIN 3")
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun setLanguage(){
        val lang = sharedPreferences.getString("Lang","en")
        Log.i("CLEAR","Language = $lang")
        val res: Resources = resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang!!.toLowerCase(Locale.ROOT)))
        res.updateConfiguration(conf, res.displayMetrics)
    }

    private fun isLanguageSelected(): String{
        val language = sharedPreferences.getString("Lang", null)
        return language ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()
        checked = 0
    }
}