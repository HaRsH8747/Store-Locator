package com.example.storelocator.sellerLogin.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentEditProfileBinding
import com.example.storelocator.util.Constants
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: Dialog
    private lateinit var Uid: String
    private var sellerData = mutableListOf<String>()
    private var mLocationPermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    private var openHour = 0
    private var openMinute = 0
    private var closeHour = 0
    private var closeMinute = 0
    private var userName = ""
    private var email =""
    private var imageUrl =""
    private var openingTime = ""
    private var closingTime = ""
    private var store = hashMapOf<Any,Any>()

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentEditProfileBinding.inflate(layoutInflater)
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog, null)
            setContentView(inflate)
            val textView = findViewById<TextView>(R.id.pdTextView)
            textView.text = getString(R.string.Updating_your_profile)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnStoreLocator.setOnClickListener {
            Constants.isLocated = false
            if (checkMapServices() && (mLocationPermissionGranted.value == true)){
                Log.i("CLEAR","Button")
                findNavController().navigate(EditProfileFragmentDirections.actionEditProfileFragmentToChangeShopLocationFragment())
            }
        }

        binding.btnOpenTime.setOnClickListener {
            addStoreOpenTiming()
        }

        binding.btnCloseTime.setOnClickListener {
            addStoreCloseTiming()
        }

        binding.btnSignUp.setOnClickListener {
            if (validateFields()){
                signUpSeller()
            }
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fis = requireContext().openFileInput(Constants.sellerFile)
        val isr = InputStreamReader(fis)
        val br = BufferedReader(isr)
        var text: String? = null
        var i = 0

        while ({ text = br.readLine(); text }() != null){
            text?.let { sellerData.add(i, it) }
            i++
        }

        userName = sellerData[0]
        email = sellerData[1]
        binding.etPhoneno.setText(sellerData[2])
        binding.etStoreName.setText(sellerData[3])
        binding.etStoreAddress.setText(sellerData[4])
        imageUrl = sellerData[5]
        if (sellerData[6].isNotEmpty()){
            binding.tvOpeningTime.text = "${getString(R.string.opening_time)}    ${sellerData[6]}"
            openingTime = sellerData[6]
        }
        if (sellerData[7].isNotEmpty()){
            binding.tvClosingTime.text = "${getString(R.string.closing_time)}    ${sellerData[7]}"
            closingTime = sellerData[7]
        }
        if (!Constants.isLocated){
            Constants.latLng = LatLng(sellerData[8].toDouble(), sellerData[9].toDouble())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        val latLng = Constants.latLng
        if (latLng != LatLng(0.0, 0.0) && Constants.isLocated){
            try {
                val geoCoder = Geocoder(requireContext())
                val address: List<Address>  = geoCoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1
                )
                binding.etStoreAddress.setText("${address[0].getAddressLine(0)}, ${address[0].locality}")
                Log.i("CLEAR","New Address: ${address[0].getAddressLine(0)}")
            }catch (e: Exception){
                Toast.makeText(requireContext(),"Unable to locate, Please try again!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addStoreOpenTiming() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { view, hourOfDay, minute ->
                openHour = hourOfDay
                openMinute = minute
                val calendar = Calendar.getInstance()
                calendar.set(0, 0, 0, openHour, openMinute)
                binding.tvOpeningTime.text = "Opening Time:    ${DateFormat.format("hh:mm aa",calendar)}"
                openingTime = DateFormat.format("hh:mm aa",calendar).toString()
            }, 12, 0, false
        )

        timePickerDialog.apply {
            updateTime(openHour,openMinute)
            show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addStoreCloseTiming() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { view, hourOfDay, minute ->
                closeHour = hourOfDay
                closeMinute = minute
                val calendar = Calendar.getInstance()
                calendar.set(0,0,0,closeHour,closeMinute)
                binding.tvClosingTime.text = "Closing Time:    ${DateFormat.format("hh:mm aa",calendar)}"
                closingTime = DateFormat.format("hh:mm aa",calendar).toString()
            },12,0,false)

        timePickerDialog.apply {
            updateTime(closeHour,closeMinute)
            show()
        }
    }

    private fun validateFields():Boolean{
        if (binding.etStoreName.text.isEmpty()){
            binding.etStoreName.error = "Please enter store name"
            return false
        }
        if (binding.etStoreAddress.text.isEmpty()){
            binding.etStoreAddress.error = "Please enter store address"
            return false
        }
        if (binding.etPhoneno.text.isEmpty()){
            binding.etPhoneno.error = "Please enter Phone No."
            return false
        }
        return true
    }

    private fun signUpSeller(){
        val storeName = binding.etStoreName.text.toString()
        val storeAddress = binding.etStoreAddress.text.toString()
        val phoneNo = binding.etPhoneno.text.toString()
        val openingTime = openingTime
        val closingTime = closingTime
            progressDialog.show()
            CoroutineScope(Dispatchers.IO).launch {
            try {
                Uid = firebaseAuth.uid.toString()
                CoroutineScope(Dispatchers.IO).launch {

                    val latitude = Constants.latLng.latitude
                    val longitude = Constants.latLng.longitude

                    store = hashMapOf(
                        "storeName" to storeName,
                        "storeAddress" to storeAddress,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "openingTime" to openingTime,
                        "closingTime" to closingTime
                    )
                    firestore.collection("sellers").document(Uid).update("phoneNo",phoneNo).await()

                    val storename = SplashActivity.sharedPreferences.getString("storeName",null)
                    val fromPath = firestore.collection("sellers").document(Uid)
                        .collection("stores").document(storename!!)
                    val toPath = firestore.collection("sellers").document(Uid)
                        .collection("stores").document(storeName)
                    var sellerData = ""
                    if (storeName != storename){
                        transferData(fromPath, toPath)
                        transferImages()
                    }else{
                        toPath.set(store, SetOptions.merge()).await()
                    }

                    sellerData = "$userName\n$email\n$phoneNo\n$storeName\n$storeAddress\n$imageUrl\n$openingTime\n$closingTime\n$latitude\n$longitude"

                    val fos = requireContext().openFileOutput(Constants.sellerFile, Context.MODE_PRIVATE)
                    fos.write(sellerData.toByteArray())
                    Log.i("CLEAR","Seller data added")
                    val sharedPreferences = SplashActivity.sharedPreferences
                    sharedPreferences.edit().putString("storeName",storeName).apply()
                    val newStore = SplashActivity.sharedPreferences.getString("storeName",null)
                    Log.i("CLEAR","new Store: $newStore")
                    progressDialog.dismiss()
                }
                }catch (e: Exception){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Log.i("CLEAR", "${e.message}")
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun transferImages() {

    }

    private fun transferData(fromPath: DocumentReference, toPath: DocumentReference) {
        CoroutineScope(Dispatchers.IO).launch {
            val task1 = fromPath.get().await()
            toPath.set(task1.data!!).await()
            val task2 = fromPath.collection("products").get().await()
            for (document in task2.documents){
                toPath.collection("products").document(document.id).set(document.data!!)
            }
            val task3 = fromPath.collection("purchaseHistory").get().await()
            for (document in task3.documents){
                toPath.collection("purchaseHistory").document(document.id).set(document.data!!)
            }
            toPath.set(store, SetOptions.merge()).await()
            fromPath.delete().await()
        }
    }

    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            if (isMapsEnabled() && getLocationPermission()) {
                mLocationPermissionGranted.value = true
                return true
            }
        }
        return false
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                val enableGpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, Constants.PERMISSIONS_REQUEST_ENABLE_GPS)
            }
        val alert = builder.create()
        alert.show()
    }

    private fun isMapsEnabled(): Boolean {
        val manager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (!manager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun getLocationPermission():Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat
                .requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            false
        }
    }

    private fun isServicesOK(): Boolean {
        Log.d("CLEAR", "isServicesOK: checking google services version")
        val available =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())
        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d("CLEAR", "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occurred but we can resolve it
            Log.d("CLEAR", "isServicesOK: an error occurred but we can fix it")
            val dialog: Dialog? = GoogleApiAvailability.getInstance()
                .getErrorDialog(requireActivity(), available, Constants.ERROR_DIALOG_REQUEST)
            dialog?.show()
        } else {
            Toast.makeText(requireContext(), "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                mLocationPermissionGranted.value = false
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mLocationPermissionGranted.value = true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

            Constants.PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i("CLEAR","Activity Result")
//                    findNavController().navigate(SellerRegistrationTwoFragmentDirections.actionSellerRegistrationTwoFragmentToRegisterShopLocation())
                } else {
                    getLocationPermission()
                }
            }
        }
    }
}