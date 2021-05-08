package com.example.storelocator.sellerLogin.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class SellerNavigationActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FindMyStore)
        setContentView(R.layout.activity_seller_navigation)
        val navView: BottomNavigationView = findViewById(R.id.seller_nav_view)
        val navController = findNavController(R.id.seller_nav_host_fragment)
        val rootActivity = findViewById<View>(R.id.sellerRootActivity)
        rootActivity.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootActivity.rootView.height - rootActivity.height
            if (heightDiff > dpToPx(this,200)){
                navView.visibility = View.GONE
            }else{
                navView.visibility = View.VISIBLE
            }
        }
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                    R.id.seller_navigation_home, R.id.seller_navigation_dashboard, R.id.seller_navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setOnNavigationItemReselectedListener {
            when(it.itemId){
                R.id.seller_navigation_home ->{
                    if (navController.currentDestination?.id == R.id.seller_navigation_home){
                        navController.popBackStack(R.id.seller_navigation_home, false)
                    }
                }

                R.id.seller_navigation_dashboard ->{
                    if (navController.currentDestination?.id == R.id.productDetailFragment){
                        navController.popBackStack(R.id.seller_navigation_dashboard, false)
                    }
                }

                R.id.seller_navigation_notifications ->{
                    if (navController.currentDestination?.id == R.id.seller_navigation_notifications){
                        navController.popBackStack(R.id.seller_navigation_notifications, false)
                    }
                }
            }
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        setLanguage()
        return super.onCreateView(name, context, attrs)
    }
    private fun setLanguage(){
        val lang = SplashActivity.sharedPreferences.getString("Lang","en")
        Log.i("CLEAR","Language = $lang")
        val res: Resources = resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang!!.toLowerCase(Locale.ROOT)))
        res.updateConfiguration(conf, res.displayMetrics)
    }

    private fun dpToPx(context: Context, valueInDp: Int): Float {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp.toFloat(), metrics)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.seller_nav_host_fragment)
        return navController.navigateUp()
    }
}