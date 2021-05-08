package com.example.storelocator.login_register.register.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.storelocator.R
import com.example.storelocator.databinding.FragmentCustomerRegisterBinding
import com.example.storelocator.mainActivity.MainActivityViewModel
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.PERMISSIONS_REQUEST_ENABLE_GPS
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.theartofdev.edmodo.cropper.CropImage
import java.io.ByteArrayOutputStream


class  CustomerRegisterFragment : Fragment() {

    private lateinit var binding: FragmentCustomerRegisterBinding
    private lateinit var navController: NavController
    private var mLocationPermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    private var uri: Uri = Uri.EMPTY
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_customer_register, container, false)
        navController = findNavController()


        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cProfileImage.setOnClickListener {
            CropImage.activity().setAspectRatio(1, 1).start(requireContext(), this)
        }

        binding.btnNext.setOnClickListener {
//            Log.i("CLEAR","btnNext: ${mainActivityViewModel.getCustomer().value}")
            val userName = binding.etUsername.text.toString()
            val email = binding.etrEmail.text.toString()
            val password = binding.etrPassword.text.toString()
            val phoneNo = binding.etPhoneno.text.toString()

            if (Constants.uri == Uri.EMPTY){
                Log.i("CLEAR","if block")
                binding.cProfileImage.setImageResource(R.drawable.ic_baseline_person_24)
                val drawable = (binding.cProfileImage.drawable as VectorDrawable)
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0,0, canvas.width, canvas.height)
                drawable.draw(canvas)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                Constants.imageData = data
            }

            if (validateFields() && checkMapServices() && (mLocationPermissionGranted.value == true) && (navController.currentDestination?.id==R.id.customerStoreRegister)){
                mainActivityViewModel.saveCustomer(Customer(userName, email, password, phoneNo))
                Log.i("CLEAR","btnNext: ${mainActivityViewModel.getCustomer().value}")
                findNavController().navigate(CustomerRegisterFragmentDirections.actionCustomerStoreRegisterToCustomerLocationFragment())
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    override fun onResume() {
        super.onResume()
        if (!uri.path.isNullOrBlank()){
            Glide.with(binding.root)
                    .load(uri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(CircleCrop())
                    .into(binding.cProfileImage)
        }
    }

    private fun validateFields():Boolean{
        val userName = binding.etUsername.text
        val email = binding.etrEmail.text
        val password = binding.etrPassword.text
        val phoneNo = binding.etPhoneno.text
        if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNo.isEmpty() || (phoneNo.length<10 || phoneNo.length>10)){
            if (binding.etUsername.text.isEmpty()){
                binding.etUsername.error = "Enter your username"
            }
            if (binding.etrEmail.text.isEmpty()){
                binding.etrEmail.error = "Enter your email address"
            }
            if (binding.etrPassword.text.isEmpty()){
                binding.etrPassword.error = "Enter your password"
            }
            if (binding.etPhoneno.text.isEmpty()){
                binding.etPhoneno.error = "Enter your phone no."
            }
            if(binding.etPhoneno.text.length < 10 || binding.etPhoneno.text.length > 10){
                binding.etPhoneno.error = "Enter valid Phone no."
            }
            return false
        }
        return true
    }

    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            if (isMapsEnabled() && getLocationPermission()) {
                mLocationPermissionGranted.value = true
                return true
            }
        }
        mLocationPermissionGranted.value = false
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

    private fun getLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted.value = true
            true
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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
            Log.d("CLEAR", "isServicesOK: an error occured but we can fix it")
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
        mLocationPermissionGranted.value = false
        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    findNavController().navigate(CustomerRegisterFragmentDirections.actionCustomerStoreRegisterToCustomerLocationFragment())
                }else{
                    mLocationPermissionGranted.value = true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("CLEAR", "onActivityResult: called.")
        when (requestCode) {

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
                            .into(binding.cProfileImage)
                }else{
                    Log.i("CLEAR","Image crop error")
                }
            }

            PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    getLocationPermission()
                }
            }
        }
    }
}