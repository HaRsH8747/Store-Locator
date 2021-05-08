package com.example.storelocator.customerLogin.profile

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentCustomerProfileBinding
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class CustomerProfileFragment : Fragment() {

    private lateinit var binding: FragmentCustomerProfileBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomerProfileBinding.inflate(inflater)

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

        binding.btnChangeLanguage.setOnClickListener {
            languageChoiceDialog.show()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val customerData = mutableListOf<String>()
        val fis = requireContext().openFileInput(Constants.customerFile)
        val isr = InputStreamReader(fis)
        val br = BufferedReader(isr)
        var text: String? = null
        var i = 0

        while ({ text = br.readLine(); text }() != null){
            text?.let { customerData.add(i, it) }
            i++
        }

        Glide.with(requireContext()).load(customerData[3]).into(binding.ivProfileImage)
        binding.tvUserName.text = customerData[0]
        binding.tvEmail.text = customerData[1]
        binding.tvPhoneNo.text = customerData[2]
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

    private fun setLanguage(lang: String = ""){
        val res: Resources = resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang.toLowerCase()))
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