package com.example.storelocator.customerLogin.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.customerLogin.productList.AllDetailProduct
import com.example.storelocator.customerLogin.productList.CustomerGridProductListAdapter
import com.example.storelocator.databinding.FragmentCustomerHomeBinding
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.cacheNearbyProductsList
import com.example.storelocator.util.Constants.Companion.cacheStoreLocationList
import com.example.storelocator.util.Constants.Companion.job1
import com.example.storelocator.util.Constants.Companion.latLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CustomerHomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentCustomerHomeBinding
    private var nearbyProductList = mutableListOf<AllDetailProduct>()
    private var productList = mutableListOf<AllDetailProduct>()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var googleMap: GoogleMap
    private var storeInfoList = mutableListOf<StoreInfo>()
    private lateinit var dialog1: Dialog
    private lateinit var dialog2: Dialog

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomerHomeBinding.inflate(layoutInflater)
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        dialog1 = Dialog(requireContext())
        dialog2 = Dialog(requireContext())
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvSeeMore.setOnClickListener {
            findNavController().navigate(CustomerHomeFragmentDirections.actionCustomerNavigationHomeToNearbyProductListFragment())
        }
        initializeMap()
    }

    override fun onResume() {
        super.onResume()
        verifyEmail()
        if (firebaseAuth.currentUser!!.isEmailVerified){
            if (cacheNearbyProductsList.isNotEmpty()){
                Log.i("CLEAR", "cachenearby")
                nearbyProductList.clear()
                nearbyProductList.addAll(cacheNearbyProductsList)
                if (nearbyProductList.size > 3){
                    binding.tvSeeMore.visibility = View.VISIBLE
                }
                binding.homeLinearProgressBar.hide()
                binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(nearbyProductList))
                binding.homeRecyclerView.adapter = gridViewAdapter
            }else{
                Log.i("CLEAR", "new nearby")
                fetchProductsFromFireStore()
            }
        }
    }

    private fun setLanguage(lang: String = ""){
        val res: Resources = resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang.toLowerCase(Locale.ROOT)))
        res.updateConfiguration(conf, res.displayMetrics)
        Log.i("CLEAR", "differ Language changed to $lang")
    }

    private fun logout(){
        setLanguage()
        val fos = requireContext().openFileOutput(Constants.userType, Context.MODE_PRIVATE)
        fos.write("".toByteArray())
        firebaseAuth.signOut()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        activity?.finish()
        Toast.makeText(requireContext(), "Logged out Successfully", Toast.LENGTH_LONG).show()
    }

    private fun verifyEmail() {
        val user = firebaseAuth.currentUser!!
        user.reload()
        Log.i("CLEAR","isVerified: ${user.isEmailVerified}\nemail: ${user.email}")
        if (!user.isEmailVerified){
            binding.homeLinearProgressBar.hide()
            dialog1.setContentView(R.layout.dialog_email_not_verified)
            dialog1.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog1.setCancelable(false)
            val btnResend = dialog1.findViewById<Button>(R.id.btnResend)
            val btnLogout = dialog1.findViewById<Button>(R.id.btnLogout)
            val tvRefresh = dialog1.findViewById<TextView>(R.id.tvRefresh)
            btnResend.setOnClickListener {
                user.reload()
                if (!user.isEmailVerified){
                    user.sendEmailVerification().addOnSuccessListener {
                        Toast.makeText(requireContext(),"Email is resent to your account\nPlease verify your Email", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(),"Error sending verification email\nPlease try again", Toast.LENGTH_LONG).show()
                        Log.i("CLEAR","Email sent error: ${it.message}")
                    }
                }else{
                    dialog1.dismiss()
                    binding.homeLinearProgressBar.hide()
                    Constants.isVerified = true
                    dialog2.setContentView(R.layout.dialog_email_verified)
                    dialog2.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    val btnOk = dialog2.findViewById<Button>(R.id.btnEmailVerified)
                    dialog2.setCancelable(false)
                    btnOk.setOnClickListener {
                        dialog2.dismiss()
                        val intent = requireActivity().intent
                        requireActivity().finish()
                        startActivity(intent)
                    }
                    dialog2.show()
                }
            }
            btnLogout.setOnClickListener {
                dialog1.dismiss()
                logout()
            }
            tvRefresh.setOnClickListener {
                dialog1.dismiss()
                val intent = requireActivity().intent
                requireActivity().finish()
                startActivity(intent)
            }
            dialog1.show()
        }
        if (!Constants.isVerified){
            if (user.isEmailVerified){
                binding.homeLinearProgressBar.hide()
                Constants.isVerified = true
                dialog2.setContentView(R.layout.dialog_email_verified)
                dialog2.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val btnOk = dialog2.findViewById<Button>(R.id.btnEmailVerified)
                btnOk.setOnClickListener {
                    dialog2.dismiss()
                }
                dialog2.show()
            }
        }
    }


    private fun initializeMap(){
        val mapFragment = childFragmentManager.findFragmentById(R.id.nearbyStoreMap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(gMap: GoogleMap?) {
        if (gMap != null) {
            googleMap = gMap
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13F))
            if (cacheStoreLocationList.isNotEmpty()){
                addStoreMarker()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.homeRefresh -> {
                binding.homeLinearProgressBar.show()
                fetchProductsFromFireStore()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchProductsFromFireStore(){
//        val userID = SplashActivity.firebaseAuth.currentUser?.uid.toString()
//        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        val storageRef = storage.reference
        job1 = CoroutineScope(Dispatchers.IO).launch {
            productList.clear()
            val querySnapshot1 = firestore.collection("sellers").get().await()
            storeInfoList.clear()
            cacheStoreLocationList.clear()
            for(productDocument1 in querySnapshot1.documents){
                //Get all sellers
                if (job1.isActive){
                    val querySnapshot2 = firestore.collection("sellers")
                            .document(productDocument1.id).collection("stores").get().await()
                    for (productDocument2 in querySnapshot2.documents){
                        //Get all stores of one user
                        val latLng = LatLng(productDocument2.get("latitude") as Double, productDocument2.get("longitude") as Double)
                        val openingTime = productDocument2.getString("openingTime") as String
                        val closingTime = productDocument2.getString("closingTime") as String
                        val storeName = productDocument2.getString("storeName") as String
                        val storeInfo = StoreInfo(storeName,latLng,openingTime,closingTime)
                        storeInfoList.add(storeInfo)
                        val querySnapshot3 = firestore.collection("sellers")
                                .document(productDocument1.id).collection("stores")
                                .document(productDocument2.id).collection("products").get().await()

                        for (productDocument3 in querySnapshot3.documents){
                            //Get all products of one store
                            val oldStoreName = productDocument2.getString("oldStoreName") as String
                            val imagesUrl = storageRef.child("seller/${productDocument1.id}/$oldStoreName/products/${productDocument3.id}.jpg").downloadUrl.await()
                            val product = AllDetailProduct(
                                    productDocument2.id,
                                    productDocument2.getString("storeAddress") as String,
                                    LatLng(productDocument2.get("latitude") as Double, productDocument2.get("longitude") as Double),
                                    productDocument2.getString("openingTime") as String,
                                    productDocument2.getString("closingTime") as String,
                                    productDocument3.getString("productName") as String,
                                    (productDocument3.get("productPrice") as Double),
                                    (productDocument3.get("discount") as Long).toInt(),
                                    (productDocument3.get("extraDiscount") as Long).toInt(),
                                    productDocument3.getString("extraOffer") as String,
                                    (productDocument3.get("popularity") as Long).toInt(),
                                    imagesUrl.toString()
                            )
                            //                        Log.i("CLEAR","product: $product")
                            productList.add(product)
                        }
                    }
                }
            }
            cacheStoreLocationList.addAll(storeInfoList)
            if (job1.isActive){
                withContext(Dispatchers.Main){
                    val customerLocation = firestore.collection("customers").document(firebaseAuth.currentUser!!.uid).get().await()
                    val latitude = customerLocation.get("latitude") as Double
                    val longitude = customerLocation.get("longitude") as Double
                    latLng = LatLng(latitude, longitude)
                    cacheNearbyProductsList.clear()
                    if (latLng != LatLng(0.0, 0.0)){
                        nearbyProductList = findNearbyProducts(productList, latLng)
                        cacheNearbyProductsList.addAll(nearbyProductList)
                    }else{
                        nearbyProductList.addAll(productList)
                        cacheNearbyProductsList.addAll(productList)
                    }
                    if (productList.isEmpty()){
                        binding.tvHomeNoProduct.visibility = View.VISIBLE
                        binding.homeLinearProgressBar.hide()
//                        binding.homeSwipeRefresh.isRefreshing = false
                    }else{
                        addStoreMarker()
                        binding.homeLinearProgressBar.hide()
                        if (nearbyProductList.size > 3){
                            binding.tvSeeMore.visibility = View.VISIBLE
                        }
                        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        val gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(nearbyProductList))
                        binding.homeRecyclerView.adapter = gridViewAdapter
//                        binding.homeSwipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun checkStoreStatus(openTime: String, closeTime: String, cTime: String): Boolean {
        Log.i("CLEAR","Open: $openTime\t Close: $closeTime\t current: $cTime")
        val opHour = openTime.substring(0,2).toInt()
        val opMin = openTime.substring(3,5).toInt()
        val opAmPm = openTime.substring(6,8)

        val clHour = closeTime.substring(0,2).toInt()
        val clMin = closeTime.substring(3,5).toInt()
        val clAmPm = closeTime.substring(6,8)

        val cHour = cTime.substring(0,2).toInt()
        val cMin = cTime.substring(3,5).toInt()
        val cAmPm = cTime.substring(7,9)

        if (opAmPm == "AM" && clAmPm == "PM"){
            if (cAmPm == "AM"){
                return if (cHour > opHour){
                    true
                } else if (cHour == opHour){
                    cMin >= opMin
                } else{
                    false
                }
            }else{
                return if (cHour < clHour){
                    true
                } else if (cHour == clHour){
                    cMin <= clMin
                } else{
                    false
                }
            }
        }
        else if (opAmPm == "PM" && clAmPm == "AM"){
            if (cAmPm == "PM"){
                return if (cHour > opHour){
                    true
                } else if (cHour == opHour){
                    cMin >= opMin
                } else{
                    false
                }
            }else{
                return if (cHour < clHour){
                    true
                } else if (cHour == clHour){
                    cMin <= clMin
                } else{
                    false
                }
            }
        }
        else if (opAmPm == "PM" && clAmPm == "PM"){
            if (cAmPm == "PM"){
                return if (cHour in (opHour + 1) until clHour){
                    true
                } else if (cHour == opHour || cHour == clHour){
                    if (cHour == opHour){
                        cMin >= opMin
                    }else{
                        cMin <= clMin
                    }
                }else{
                    false
                }
            }else{
                return if (cHour in (opHour + 1) until clHour){
                    true
                } else if (cHour == opHour || cHour == clHour){
                    if (cHour == opHour){
                        cMin >= opMin
                    }else{
                        cMin <= clMin
                    }
                }else{
                    false
                }
            }
        }
        else if (opAmPm == "AM" && clAmPm == "AM"){
            if (cAmPm == "AM"){
                return if (cHour in (opHour + 1) until clHour){
                    true
                } else if (cHour == opHour || cHour == clHour){
                    if (cHour == opHour){
                        cMin >= opMin
                    }else{
                        cMin <= clMin
                    }
                }else{
                    false
                }
            }else{
                return if (cHour in (opHour + 1) until clHour){
                    true
                } else if (cHour == opHour || cHour == clHour){
                    if (cHour == opHour){
                        cMin >= opMin
                    }else{
                        cMin <= clMin
                    }
                }else{
                    false
                }
            }
        }
        return false
    }

    private fun addStoreMarker(){
        googleMap.apply {
            if (cacheStoreLocationList.isNotEmpty()){
                val storeGeoCoder = Geocoder(requireContext())
                val customerAddress: List<Address>  = storeGeoCoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1
                )
                addMarker(
                        MarkerOptions()
                                .position(latLng)
                                .title("You are here")
                                .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_placeholder))
                )
                var i = 0
                Log.i("CLEAR", "$cacheStoreLocationList")
                val localTime = LocalDateTime.now()
                val cTime = localTime.format(DateTimeFormatter.ofPattern("hh:mm: a")).toString()
                for (storeLocation in cacheStoreLocationList){
                    val storeAddress = storeGeoCoder.getFromLocation(storeLocation.latLng.latitude, storeLocation.latLng.longitude, 1)
                    var status = ""
                    status = if (checkStoreStatus(storeLocation.openingTime,storeLocation.closingTime,cTime)){
                        "Store is Open"
                    }else{
                        "Store is Closed"
                    }
                    if (storeAddress[0].postalCode == customerAddress[0].postalCode){
                        addMarker(
                                MarkerOptions()
                                        .position(storeLocation.latLng)
                                        .title(cacheStoreLocationList[i].storeName)
                                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_store_location_icon))
                                        .title(status)
                                        .snippet("Store Timing: ${storeLocation.openingTime} - ${storeLocation.closingTime}")
                        )
                        i += 1
                    }
                }
                animateCamera(CameraUpdateFactory.newLatLng(latLng))
                animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13F))
            }
        }
    }

    private suspend fun findNearbyProducts(list: MutableList<AllDetailProduct>, latLng: LatLng): MutableList<AllDetailProduct> {
        try {
            val geoCoder = Geocoder(requireContext())
            val customerAddress: List<Address>  = geoCoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1
            )
            for (i in 0 until list.size){
                val platLng = list[i].storeLatLng
                if (platLng != LatLng(0.0, 0.0)){
                    val productAddress = geoCoder.getFromLocation(platLng.latitude, platLng.longitude, 1)
                    if (productAddress[0].postalCode != customerAddress[0].postalCode){
                        list.removeAt(i)
                    }
                }
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Log.i("CLEAR", "Unable to locate, Please try again!")
            }
        }
        return list
    }

    private fun sortByPopularity(list: MutableList<AllDetailProduct>): MutableList<AllDetailProduct>{
        var temp: AllDetailProduct
        for (i in 0 until (list.size-1)){
            for (j in 0 until (list.size-i-1)){
                if (list[j].popularity < list[j + 1].popularity){
                    temp = list[j]
                    list[j] = list[j + 1]
                    list[j + 1] = temp
                }
            }
        }
        return list
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}