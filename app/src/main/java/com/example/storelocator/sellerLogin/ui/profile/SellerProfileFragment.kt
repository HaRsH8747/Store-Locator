package com.example.storelocator.sellerLogin.ui.profile

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentSellerProfileBinding
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


class SellerProfileFragment : Fragment() {

//  private lateinit var sellerProfileViewModel: SellerProfileViewModel
    private lateinit var binding: FragmentSellerProfileBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var sellerData = mutableListOf<String>()
    private var openHour = 0
    private var openMinute = 0
    private var closeHour = 0
    private var closeMinute = 0

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
//      sellerProfileViewModel =
//              ViewModelProvider(this).get(SellerProfileViewModel::class.java)
//      sellerProfileViewModel.text.observe(viewLifecycleOwner, {
//      })

      binding = FragmentSellerProfileBinding.inflate(layoutInflater)
      firebaseAuth = SplashActivity.firebaseAuth
      firestore = FirebaseFirestore.getInstance()
      storage = FirebaseStorage.getInstance()
      var language = ""

      val currentLang = SplashActivity.sharedPreferences.getString("Lang","en")
      var checkedItem = 0
      when(currentLang){
          "en" -> checkedItem = 0
          "hi" -> checkedItem = 1
          "gu" -> checkedItem = 2
      }
      val options = arrayOf("English","हिंदी","ગુજરાતી")
      val languageChoiceDialog = AlertDialog.Builder(requireContext())
              .setTitle("Select Language")
              .setSingleChoiceItems(options, checkedItem){dialog, which ->
                  when(options[which]){
                      "English" -> language = "en"
                      "हिंदी" -> language = "hi"
                      "ગુજરાતી" -> language = "gu"
                  }
              }
              .setPositiveButton("Change"){dialog, which ->
                    setLanguageAndRestart(language)
              }
              .setNegativeButton("Cancel"){dialog, which ->}
              .create()

      binding.btnEditProfile.setOnClickListener {
          findNavController().navigate(SellerProfileFragmentDirections.actionSellerNavigationNotificationsToEditProfileFragment())
      }

      binding.btnChangeLanguage.setOnClickListener {
          languageChoiceDialog.show()
      }

      binding.btnQRCode.setOnClickListener {
          openQRCode()
      }

      binding.ivOpenTime.setOnClickListener {
          addStoreOpenTiming()
      }

      binding.ivCloseTime.setOnClickListener {
          addStoreCloseTiming()
      }

      binding.btnLogout.setOnClickListener {
          logout()
      }

      return binding.root
  }

    private fun setLanguageAndRestart(lang: String){
        val sharedPreferences = SplashActivity.sharedPreferences
        sharedPreferences.edit().putString("Lang",lang).apply()
        val res: Resources = requireContext().resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang.toLowerCase(Locale.ROOT)))
        res.updateConfiguration(conf, res.displayMetrics)
        val intent = requireActivity().intent
        requireActivity().finish()
        startActivity(intent)
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
                val storeName = SplashActivity.sharedPreferences.getString("storeName", null)
                firestore.collection("sellers").document(firebaseAuth.currentUser!!.uid)
                    .collection("stores").document(storeName!!)
                    .update("openingTime",DateFormat.format("hh:mm a",calendar)).addOnSuccessListener {
                        writeOpenTimeToSellerFile(DateFormat.format("hh:mm a",calendar) as String)
                        binding.tvOpeningTime.text = "${getString(R.string.opening_time)}    ${DateFormat.format("hh:mm a",calendar)}"
                    }
            }, 12, 0, false
        )

        timePickerDialog.apply {
            updateTime(openHour,openMinute)
            show()
        }
    }

    private fun writeOpenTimeToSellerFile(time: String) {
        val userName = sellerData[0]
        val email = sellerData[1]
        val phoneNo = sellerData[2]
        val storeName = sellerData[3]
        val storeAddress = sellerData[4]
        val imageUrl = sellerData[5]
        sellerData[6] = time
        val closingTime = sellerData[7]
        val sellerData = "$userName\n$email\n$phoneNo\n$storeName\n$storeAddress\n$imageUrl\n$time\n$closingTime"
        val fos = requireContext().openFileOutput(Constants.sellerFile, Context.MODE_PRIVATE)
        fos.write(sellerData.toByteArray())
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
                val storeName = SplashActivity.sharedPreferences.getString("storeName", null)
                firestore.collection("sellers").document(firebaseAuth.currentUser!!.uid)
                    .collection("stores").document(storeName!!)
                    .update("closingTime",DateFormat.format("hh:mm aa",calendar)).addOnSuccessListener {
                            writeCloseTimeToSellerFile(DateFormat.format("hh:mm a",calendar) as String)
                        binding.tvClosingTime.text = "${getString(R.string.closing_time)}     ${DateFormat.format("hh:mm aa",calendar)}"
                    }
            },12,0,false)

        timePickerDialog.apply {
            updateTime(closeHour,closeMinute)
            show()
        }
    }

    private fun writeCloseTimeToSellerFile(time: String) {
        val userName = sellerData[0]
        val email = sellerData[1]
        val phoneNo = sellerData[2]
        val storeName = sellerData[3]
        val storeAddress = sellerData[4]
        val imageUrl = sellerData[5]
        val openingTime = sellerData[6]
        sellerData[7] = time
        val sellerData = "$userName\n$email\n$phoneNo\n$storeName\n$storeAddress\n$imageUrl\n$openingTime\n$time"
        val fos = requireContext().openFileOutput(Constants.sellerFile, Context.MODE_PRIVATE)
        fos.write(sellerData.toByteArray())
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

        binding.tvUserName.text = sellerData[0]
        binding.tvEmail.text = sellerData[1]
        binding.tvPhoneNo.text = sellerData[2]
        binding.tvStoreName.text = sellerData[3]
        binding.tvStoreAddress.text = sellerData[4]
        Glide.with(requireContext()).load(sellerData[5]).into(binding.profileImage)
        if (sellerData[6].isNotEmpty()){
            binding.tvOpeningTime.text = "Opening Time:    ${sellerData[6]}"
        }
        if (sellerData[7].isNotEmpty()){
            binding.tvClosingTime.text = "ClosingTime:    ${sellerData[7]}"
        }
    }

    private fun logout(){
        addPreference()
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

    private fun openQRCode(){
        findNavController().navigate(SellerProfileFragmentDirections.actionSellerNavigationNotificationsToQRCodeGenerator())
    }

    private fun setLanguage(lang: String = ""){
        val res: Resources = resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang.toLowerCase(Locale.ROOT)))
        res.updateConfiguration(conf, res.displayMetrics)
        Log.i("CLEAR", "differ Language changed to $lang")
    }

    private fun addPreference(lang: String = ""){
        val editor = SplashActivity.sharedPreferences.edit()
        editor.apply {
            putString("Lang", lang)
        }.apply()
    }
}