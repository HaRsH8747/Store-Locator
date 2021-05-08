package com.example.storelocator.customerLogin

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.util.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class CustomerNavigationActivity : AppCompatActivity() {

//    private lateinit var binding: ActivityCustomerNavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityCustomerNavigationBinding.inflate(layoutInflater)
//        setTheme(R.style.Theme_FindMyStore)
        setContentView(R.layout.activity_customer_navigation)
        val navView: BottomNavigationView = findViewById(R.id.customer_nav_view)
        val navController = findNavController(R.id.customer_nav_host_fragment)
        val rootActivity = findViewById<ConstraintLayout>(R.id.customerRootActivity)
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
                    R.id.customer_navigation_home, R.id.customer_navigation_product_list, R.id.customer_navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setOnNavigationItemReselectedListener {
            when(it.itemId){
                R.id.customer_navigation_home ->{
                    if (navController.currentDestination?.id == R.id.customer_navigation_home){
                        Log.i("CLEAR","Home Reselected")
                        navController.popBackStack(R.id.customer_navigation_home, false)
                    }
                }

                R.id.customer_navigation_product_list ->{
                    if (navController.currentDestination?.id == R.id.customerProductDetailFragment
                            || navController.currentDestination?.id == R.id.QRScannerFragment){
                        Log.i("CLEAR","Product List Reselected")
                        navController.popBackStack(R.id.customer_navigation_product_list, false)
                    }
                }

                R.id.customer_navigation_profile ->{
                    if (navController.currentDestination?.id == R.id.customer_navigation_profile){
                        Log.i("CLEAR","Profile Reselected")
                        navController.popBackStack(R.id.customer_navigation_profile, false)
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
        val navController = this.findNavController(R.id.customer_nav_host_fragment)
        Log.i("CLEAR","navigate")
        return navController.navigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        Constants.cacheAllProductsList.clear()
        Constants.cacheNearbyProductsList.clear()
        Constants.cacheStoreLocationList.clear()
    }
}