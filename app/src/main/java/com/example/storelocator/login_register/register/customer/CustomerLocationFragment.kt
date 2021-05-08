package com.example.storelocator.login_register.register.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.customerLogin.CustomerNavigationActivity
import com.example.storelocator.databinding.FragmentCustomerLocationBinding
import com.example.storelocator.mainActivity.MainActivityViewModel
import com.example.storelocator.util.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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

class CustomerLocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentCustomerLocationBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var lastLocation: Location
    private lateinit var progressDialog: Dialog
    private lateinit var googleMap: GoogleMap
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var userName: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var phoneNo: String
    private lateinit var UserID: String
    private lateinit var progressDialog1: Dialog
    private lateinit var dialog: Dialog
    private var imageUploaded = MutableLiveData<Boolean>()

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCustomerLocationBinding.inflate(layoutInflater)
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        imageUploaded.value = false

        dialog = Dialog(requireContext())
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.customer_progress_dialog, null)
            setContentView(inflate)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        progressDialog1 = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_signin, null)
            setContentView(inflate)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeMap()
//        Log.i("CLEAR","fused Loc: $fusedLocationProviderClient")

        val customer = mainActivityViewModel.getCustomer()
        Log.i("CLEAR","customer: ${customer.value}")
        userName = customer.value!!.userName
        email = customer.value!!.email
        password = customer.value!!.password
        phoneNo = customer.value!!.phoneNo

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.create().apply {
            interval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                lastLocation = locationResult.lastLocation
                Log.i("CLEAR","lastLocation:- $lastLocation")
                googleMap.apply {
                    val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    Log.i("CLEAR", "Shop: ${lastLocation.latitude}\n Longitude: ${lastLocation.longitude}")
                    googleMap.clear()
                    addMarker(
                            MarkerOptions()
                                    .position(latLng)
                                    .title("You are here")
                    )
                    animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
                    stopLocationUpdates()
                    progressDialog.dismiss()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSignUp.setOnClickListener {
            signUp()
        }
    }

    override fun onResume() {
        super.onResume()
        progressDialog.show()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }


    private fun startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun initializeMap(){
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
    }

    private fun signUp(){
        var imageUrl: Uri
        if(email.isNotEmpty() && password.isNotEmpty()){
            progressDialog1.show()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            CoroutineScope(Dispatchers.IO).launch {
                            UserID = firebaseAuth.uid.toString()
                            val storageRef = storage.reference
//                            uploadProfilePicture()
                            val imagesRef = storageRef.child("customer/$UserID.jpg")
                            if (Constants.uri == Uri.EMPTY){
                                imagesRef.putBytes(Constants.imageData).await()
                            }else{
                                imagesRef.putFile(Constants.uri).await()
                            }

                            val user = hashMapOf(
                                "userName" to userName,
                                "email" to email,
                                "phoneNo" to phoneNo,
                                "isCustomer" to 1,
                                "latitude" to lastLocation.latitude,
                                "longitude" to lastLocation.longitude
                            )

                            Log.i("CLEAR","Adding user")
                            firestore.collection("customers").document(UserID).set(user).await()
                            imageUrl = storageRef.child("customer/$UserID.jpg").downloadUrl.await()
                            val customerData = "$userName\n$email\n$phoneNo\n$imageUrl"

                            withContext(Dispatchers.Main){
                                Log.i("CLEAR","$customerData")
                                val fos1 = requireContext().openFileOutput(Constants.customerFile, Context.MODE_PRIVATE)
                                fos1.write(customerData.toByteArray())
                                Log.i("CLEAR","coroutine 2")
                                val fos2 = requireContext().openFileOutput(Constants.userType, Context.MODE_PRIVATE)
                                fos2.write("customer".toByteArray())

                                val fuser = firebaseAuth.currentUser
                                fuser!!.sendEmailVerification().addOnSuccessListener {
                                    progressDialog.dismiss()
                                    dialog.setContentView(R.layout.dialog_email_sent)
                                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                    dialog.setCancelable(false)
                                    val button = dialog.findViewById<Button>(R.id.btnEmailSent)
                                    button.setOnClickListener {
                                        progressDialog1.dismiss()
                                        dialog.dismiss()
                                        val intent = Intent(requireContext(), CustomerNavigationActivity::class.java)
                                        startActivity(intent)
                                        requireActivity().finish()
                                        Toast.makeText(requireContext(),"Registration Successful",Toast.LENGTH_LONG).show()
                                    }
                                    dialog.show()
                                }.addOnFailureListener {
                                    progressDialog1.dismiss()
                                    Toast.makeText(requireContext(),"Error sending verification email\nPlease try again",Toast.LENGTH_LONG).show()
                                    Log.i("CLEAR","Email sent error: ${it.message}")
                                }
                            }
                            }
                        }else{
                            progressDialog1.dismiss()
                            Log.i("CLEAR", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(context, "Invalid Entry or User is already Registered", Toast.LENGTH_SHORT).show()
                        }
                    }

                }catch (e: Exception){
                    withContext(Dispatchers.Main){
                        progressDialog1.dismiss()
                        Toast.makeText(requireContext(),e.message, Toast.LENGTH_LONG).show()
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
}