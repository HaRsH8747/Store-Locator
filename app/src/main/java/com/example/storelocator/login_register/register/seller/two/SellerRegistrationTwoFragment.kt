package com.example.storelocator.login_register.register.seller.two

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentSellerRegisterTwoBinding
import com.example.storelocator.mainActivity.MainActivityViewModel
import com.example.storelocator.sellerLogin.ui.SellerNavigationActivity
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.ERROR_DIALOG_REQUEST
import com.example.storelocator.util.Constants.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.example.storelocator.util.Constants.Companion.PERMISSIONS_REQUEST_ENABLE_GPS
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*


class SellerRegistrationTwoFragment: Fragment(){

    private lateinit var binding: FragmentSellerRegisterTwoBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
//    private val ACCESS_FINE_LOCATION = 1
    private lateinit var locationManager: LocationManager
    private var uri: Uri = Uri.EMPTY
//    private lateinit var sharedPreferences: SharedPreferences
//    private var fineLocationGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    private var mLocationPermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var userName: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var phoneNo: String
    private lateinit var Uid: String
    private lateinit var progressDialog: Dialog
    private lateinit var dialog: Dialog
    private var imageUploaded = MutableLiveData<Boolean>()
    private var openHour = 0
    private var openMinute = 0
    private var closeHour = 0
    private var closeMinute = 0
    private var openingTime = ""
    private var closingTime = ""

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_seller_register_two,
            container,
            false
        )
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        imageUploaded.value = false

        dialog = Dialog(requireContext())
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_signin, null)
            setContentView(inflate)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mainActivityViewModel.seller.observe(viewLifecycleOwner, { seller ->
            userName = seller.userName
            email = seller.email
            password = seller.password
            phoneNo = seller.phoneNo
            Log.i("CLEAR", "$seller $email, $password")
        })

        if (Constants.isLocated){
            binding.etStoreAddress.visibility = View.VISIBLE
        }

        binding.profileImage.setOnClickListener {
            CropImage.activity().setAspectRatio(1, 1).start(requireContext(), this)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigate(SellerRegistrationTwoFragmentDirections.actionSellerRegistrationTwoFragmentToSellerRegistrationFragment())
        }

        binding.btnStoreLocator.setOnClickListener {
            Constants.isLocated = false
            if (checkMapServices() && (mLocationPermissionGranted.value == true)){
                Log.i("CLEAR","Button")
                findNavController().navigate(SellerRegistrationTwoFragmentDirections.actionSellerRegistrationTwoFragmentToRegisterShopLocation())
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
    override fun onResume() {
        super.onResume()
        val latLng = Constants.latLng
        Log.i("CLEAR","geo latlng: - $latLng")
        if (latLng != LatLng(0.0, 0.0)){
            try {
                val geoCoder = Geocoder(requireContext())
                val address: List<Address>  = geoCoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1
                )
                binding.etStoreAddress.setText("${address[0].getAddressLine(0)}, ${address[0].locality}")
            }catch (e: Exception){
                Toast.makeText(requireContext(),"Unable to locate, Please try again!",Toast.LENGTH_LONG).show()
            }
            Log.i("CLEAR","uri:- $uri")
            if (!uri.path.isNullOrBlank()){
                Glide.with(binding.root)
                        .load(uri)
                        .skipMemoryCache(true) //2
                        .diskCacheStrategy(DiskCacheStrategy.NONE) //3
                        .transform(CircleCrop()) //4
                        .into(binding.profileImage)
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
        return true
    }

    private fun signUpSeller(){
        val storeName = binding.etStoreName.text.toString()
        val storeAddress = binding.etStoreAddress.text.toString()
        val openingTime = openingTime
        val closingTime = closingTime
        var imageUrl: Uri
        if (email.isNotEmpty() && password.isNotEmpty()){
            progressDialog.show()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    Log.i("CLEAR", "$email, $password")
                        if (task.isSuccessful){
                            Uid = firebaseAuth.uid.toString()
                        CoroutineScope(Dispatchers.IO).launch {
                            val storageRef = storage.reference
                            val imagesRef = storageRef.child("seller/$Uid.jpg")
                            withContext(Dispatchers.Main){
                                if (uri == Uri.EMPTY){
                                    Log.i("CLEAR","if block")
                                    binding.profileImage.setImageResource(R.drawable.ic_baseline_person_24)
                                    val drawable = (binding.profileImage.drawable as VectorDrawable)
                                    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    drawable.setBounds(0,0, canvas.width, canvas.height)
                                    drawable.draw(canvas)
                                    val baos = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                    val data = baos.toByteArray()
                                    imagesRef.putBytes(data).await()
                                }else{
                                    imagesRef.putFile(uri).await()
                                }
                            }
                            val seller = hashMapOf(
                                "userName" to userName,
                                "email" to email,
                                "phoneNo" to phoneNo,
                                "isSeller" to 1
                            )

                            val latitude = Constants.latLng.latitude
                            val longitude = Constants.latLng.longitude

                            val store = hashMapOf(
                                "storeName" to storeName,
                                "storeAddress" to storeAddress,
                                "latitude" to latitude,
                                "longitude" to longitude,
                                "openingTime" to openingTime,
                                "closingTime" to closingTime,
                                "oldStoreName" to storeName
                            )
                                firestore.collection("sellers").document(Uid).set(seller).await()

                                firestore.collection("sellers").document(Uid)
                                    .collection("stores").document(storeName).set(store).await()

                                imageUrl = storageRef.child("seller/$Uid.jpg").downloadUrl.await()

                                val sellerData = "$userName\n$email\n$phoneNo\n$storeName\n$storeAddress\n$imageUrl\n$openingTime\n$closingTime\n$latitude\n$longitude"

                                val fos = requireContext().openFileOutput(Constants.sellerFile, Context.MODE_PRIVATE)
                                fos.write(sellerData.toByteArray())
                                val fos2 = requireContext().openFileOutput(Constants.userType, Context.MODE_PRIVATE)
                                fos2.write("seller".toByteArray())
                                Log.i("CLEAR","Seller data added")
                                val sharedPreferences = requireActivity().getSharedPreferences(Constants.MY_SHARED_PREF, AppCompatActivity.MODE_PRIVATE)
                                sharedPreferences.edit().putString("storeName",storeName).apply()

                                val user = firebaseAuth.currentUser
                                user!!.sendEmailVerification().addOnSuccessListener {
                                    progressDialog.dismiss()
                                    dialog.setContentView(R.layout.dialog_email_sent)
                                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                    dialog.setCancelable(false)
                                    val button = dialog.findViewById<Button>(R.id.btnEmailSent)
                                    button.setOnClickListener {
                                        dialog.dismiss()
                                        val intent = Intent(requireContext(), SellerNavigationActivity::class.java)
                                        startActivity(intent)
                                        Constants.QR_CODE_GENERATED_FIRST_TIME = true
                                        requireActivity().finish()
                                    }
                                    dialog.show()
                                }.addOnFailureListener {
                                    progressDialog.dismiss()
                                    Toast.makeText(requireContext(),"Error sending verification email\nPlease try again",Toast.LENGTH_LONG).show()
                                    Log.i("CLEAR","Email sent error: ${it.message}")
                                }
                            }
                        }else{
                            progressDialog.dismiss()
                            Log.i("CLEAR", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                context,
                                "Invalid Entry or User is already Registered",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
    }

    private fun uploadProfilePicture(){
        CoroutineScope(Dispatchers.IO).launch {
            val storageRef = storage.reference
            withContext(Dispatchers.Main){
                imageUploaded.value = true
            }
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
                startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
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
                    .requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
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
                .getErrorDialog(requireActivity(), available, ERROR_DIALOG_REQUEST)
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
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
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

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null){
                    val result = CropImage.getActivityResult(data)
                    uri = result.uri
                    Log.i("CLEAR", "URI:$uri")
                    Glide.with(binding.root)
                        .load(uri)
                        .skipMemoryCache(true) //2
                        .diskCacheStrategy(DiskCacheStrategy.NONE) //3
                        .transform(CircleCrop()) //4
                        .into(binding.profileImage)
                }else{
                    Log.i("CLEAR","Image crop error")
                }
            }

            PERMISSIONS_REQUEST_ENABLE_GPS -> {
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





















    //    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun getFineLocation(){
//        if (ContextCompat.checkSelfPermission(
//                requireActivity(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(
//                    requireActivity(),
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED){
//            //Permission not Granted
//                Log.i("CLEAR", "Permission not Granted")
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    requireActivity(),
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) &&
//                    ActivityCompat.shouldShowRequestPermissionRationale(
//                        requireActivity(),
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    )){
//                //can ask user for permission
//                Log.i("CLEAR", "can ask user for permission")
//                requestPermissions(
//                    arrayOf(
//                        Manifest.permission.ACCESS_FINE_LOCATION,
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ), ACCESS_FINE_LOCATION
//                )
//
//            }else{
//                //User checked "Don't Ask again" or First time permission request
//                Log.i("CLEAR", "User checked \"Don't Ask again\" or First time permission request")
//                val userAskedPermissionBefore = sharedPreferences.getBoolean(
//                    Constants.USER_ASKED_STORAGE_PERMISSION_BEFORE,
//                    false
//                )
//                Log.i("CLEAR", userAskedPermissionBefore.toString())
//
//                if (userAskedPermissionBefore) {
//                    //If User was asked permission before and denied and also checked "Don't ask again"
//                    Log.i(
//                        "CLEAR",
//                        "If User was asked permission before and denied and also checked \"Don't ask again\""
//                    )
//                    val alertDialog = AlertDialog.Builder(requireContext()).apply {
//                        setTitle("Permission needed")
//                        setMessage("Location Permission needed for accessing device location")
//                        setPositiveButton(
//                            "Open Settings",
//                            DialogInterface.OnClickListener { dialog, which ->
//                                val intent = Intent().apply {
//                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                                    val uri = Uri.fromParts("package", activity?.packageName, null)
//                                    data = uri
//                                }
//                                requireActivity().startActivity(intent)
//                            })
//
//                        setNegativeButton(
//                            "Cancel",
//                            DialogInterface.OnClickListener { dialog, which ->
//                                Log.i("CLEAR", "Cancelling")
//                            })
//                    }.create()
//                    alertDialog.show()
//
//                }else{
//                    //If user is asked permission for first time
//                    Log.i("CLEAR", "If user is asked permission for first time")
//                    requestPermissions(
//                        arrayOf(
//                            Manifest.permission.ACCESS_FINE_LOCATION,
//                            Manifest.permission.ACCESS_COARSE_LOCATION
//                        ), ACCESS_FINE_LOCATION
//                    )
//                    sharedPreferences.edit().apply {
//                        putBoolean(Constants.USER_ASKED_STORAGE_PERMISSION_BEFORE, true)
//                        Log.i("CLEAR", "First time")
//                        apply()
//                    }
//                }
//            }
//
//        }else{
//            fineLocationGranted.value = true
//            getGps()
//        }
//    }






    //    @SuppressLint("SetTextI18n")
//    override fun onResume() {
//        super.onResume()
//        val latLng = Constants.latLng
//        if (latLng != LatLng(0.0, 0.0)){
//            val geoCoder = Geocoder(requireContext(), Locale.getDefault())
//            val address: MutableList<Address> = geoCoder.getFromLocation(
//                latLng.latitude, latLng.longitude, 1
//            )
//            binding.etStoreAddress.setText("${address[0].getAddressLine(0)}, ${address[0].locality}")
//            Log.i("CLEAR", "Address Added")
//        }
//
//        if (checkMapServices() && !Constants.isLocated){
//            if (mLocationPermissionGranted){
//                findNavController().navigate(SellerRegistrationTwoFragmentDirections.actionSellerRegistrationTwoFragmentToRegisterShopLocation())
//            }else{
//                getLocationPermission()
//            }
//        }
//    }
}