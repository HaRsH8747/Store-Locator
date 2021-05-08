package com.example.storelocator.sellerLogin.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.storelocator.R
import com.example.storelocator.databinding.FragmentChangeShopLocationBinding
import com.example.storelocator.util.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ChangeShopLocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentChangeShopLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var lastLocation: Location
    private lateinit var progressDialog: Dialog
    private lateinit var googleMap: GoogleMap

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangeShopLocationBinding.inflate(layoutInflater)

        Constants.isLocated = true
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog, null)
            setContentView(inflate)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
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
                    Constants.isLocated = true
                    Constants.latLng = latLng
                    animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
                    stopLocationUpdates()
                    progressDialog.dismiss()
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeMap()
        Log.i("CLEAR","fused Loc: $fusedLocationProviderClient")
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
        googleMap.setOnMapLongClickListener {
            googleMap.clear()
            googleMap.apply {
                addMarker(
                    MarkerOptions()
                        .position(it).
                        title("Store Location")
                )
                Constants.isLocated = true
                Constants.latLng = it
                Log.i("CLEAR","new latlng: ${Constants.latLng}")
                animateCamera(CameraUpdateFactory.newLatLng(it))
                animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15F))
            }
        }
    }
}