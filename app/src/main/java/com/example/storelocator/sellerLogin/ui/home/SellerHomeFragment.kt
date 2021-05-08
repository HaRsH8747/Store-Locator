package com.example.storelocator.sellerLogin.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentSellerHomeBinding
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.util.Constants
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class SellerHomeFragment : Fragment() {

  private lateinit var sellerHomeViewModel: SellerHomeViewModel
  private lateinit var binding: FragmentSellerHomeBinding
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var dialog1: Dialog
  private lateinit var dialog2: Dialog
  private var isVerified = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

      binding = FragmentSellerHomeBinding.inflate(inflater)
      firebaseAuth = FirebaseAuth.getInstance()
      dialog1 = Dialog(requireContext())
      dialog2 = Dialog(requireContext())

      binding.imgbtnAddProduct.setOnClickListener {
          findNavController().navigate(SellerHomeFragmentDirections.actionSellerNavigationHomeToAddProductFragment())
      }

      binding.imgbtnEditProduct.setOnClickListener {
          findNavController().navigate(SellerHomeFragmentDirections.actionSellerNavigationHomeToEditProductFragment())
      }

      binding.imgbtnPurchaseHistory.setOnClickListener {
          findNavController().navigate(SellerHomeFragmentDirections.actionSellerNavigationHomeToPurchaseHistoryFragment())
      }
      return binding.root
  }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("CLEAR","onViewCreated")
//        if (Constants.QR_CODE_GENERATED_FIRST_TIME){
//            Constants.QR_CODE_GENERATED_FIRST_TIME = false
//            findNavController().navigate(SellerHomeFragmentDirections.actionSellerNavigationHomeToQRCodeGenerator())
//        }
    }

    override fun onResume() {
        super.onResume()
        checkLanguage()
        verifyEmail()
    }

    private fun checkLanguage() {
        val lang = SplashActivity.sharedPreferences.getString("Lang","en")
        if (lang == "hi"){
            binding.imgbtnAddProduct.setImageResource(R.drawable.ic_add_product_hindi)
            binding.imgbtnEditProduct.setImageResource(R.drawable.ic_edit_product_hindi)
            binding.imgbtnPurchaseHistory.setImageResource(R.drawable.ic_purchase_history_hindi)
        }
        if (lang == "gu"){
            binding.imgbtnAddProduct.setImageResource(R.drawable.ic_add_product_gujarati)
            binding.imgbtnEditProduct.setImageResource(R.drawable.ic_edit_product_gujarati)
            binding.imgbtnPurchaseHistory.setImageResource(R.drawable.ic_purchase_history_gujarati)
        }
    }

    private fun verifyEmail() {
        val user = firebaseAuth.currentUser!!
        user.reload()
        Log.i("CLEAR","isVerified: ${user.isEmailVerified}\nemail: ${user.email}")
        if (!user.isEmailVerified){
            dialog1.setContentView(R.layout.dialog_email_not_verified)
            dialog1.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog1.setCancelable(false)
            val btnResend = dialog1.findViewById<Button>(R.id.btnResend)
            val btnLogout = dialog1.findViewById<Button>(R.id.btnLogout)
            val tvRefresh = dialog1.findViewById<TextView>(R.id.tvRefresh)
            btnResend.setOnClickListener {
                if (!user.isEmailVerified){
                    dialog1.dismiss()
                    user.sendEmailVerification().addOnSuccessListener {
                        Toast.makeText(requireContext(),"Email is resent to your account\nPlease verify your Email", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(),"Error sending verification email\nPlease try again", Toast.LENGTH_LONG).show()
                        Log.i("CLEAR","Email sent error: ${it.message}")
                    }
                }else{
                    dialog1.dismiss()
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
                logout()
            }
            tvRefresh.setOnClickListener {
                val intent = requireActivity().intent
                requireActivity().finish()
                startActivity(intent)
            }
            dialog1.show()
        }
        if (!Constants.isVerified){
            if (user.isEmailVerified){
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

    override fun onDestroy() {
        super.onDestroy()
        Log.i("CLEAR","onDestroy")
    }
}