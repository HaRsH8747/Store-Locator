package com.example.storelocator.login_register.login

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.customerLogin.CustomerNavigationActivity
import com.example.storelocator.databinding.FragmentStoreLoginBinding
import com.example.storelocator.mainActivity.MainActivity
import com.example.storelocator.sellerLogin.ui.SellerNavigationActivity
import com.example.storelocator.util.Constants
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentStoreLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var mainActivity: MainActivity
    private lateinit var args: LoginFragmentArgs
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: Dialog


    @SuppressLint("InflateParams", "ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =DataBindingUtil.inflate(inflater, R.layout.fragment_store_login, container,false)
        mainActivity = this.activity as MainActivity
        navController = findNavController()
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        progressDialog = Dialog(requireContext()).apply {
            val inflate = LayoutInflater.from(requireContext()).inflate(R.layout.progress_dialog_login, null)
            setContentView(inflate)
            setCancelable(false)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        args = LoginFragmentArgs.fromBundle(requireArguments())
//        val sharedPreferences = requireActivity().getSharedPreferences(Constants.MY_SHARED_PREF, Context.MODE_PRIVATE)
        binding.btnLogin.setOnClickListener {
            loginUser()
        }
        binding.tvSignUp.setOnClickListener {
            navController.navigate(LoginFragmentDirections.actionStoreLoginToRegisterRoleSelector())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity.supportActionBar?.title = getString(R.string.store_login)
    }

    private fun validateFields():Boolean{
        if (binding.etEmail.text.isEmpty() || binding.etPassword.text.isEmpty()){
            if (binding.etEmail.text.isEmpty()){
                binding.etEmail.error = "Enter your email"
            }
            if (binding.etPassword.text.isEmpty()){
                binding.etPassword.error = "Enter Password"
            }
            return false
        }
        return true
    }


    private fun loginUser(){
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        if(validateFields()){
            progressDialog.show()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseAuth.signInWithEmailAndPassword(email, password).await()
                    checkUserAccessLevel()
                    checkLoggedInState()
                }catch (e: FirebaseAuthInvalidUserException){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Toast.makeText(requireActivity(),"Invalid Email or Password", Toast.LENGTH_LONG).show()
                    }
                }catch (e: FirebaseAuthInvalidCredentialsException){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Toast.makeText(requireActivity(),"Invalid Email or Password", Toast.LENGTH_LONG).show()
                    }
                }catch (e: FirebaseTooManyRequestsException){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Toast.makeText(requireActivity(),"Multiple requests to this account. Try again later", Toast.LENGTH_LONG).show()
                    }
                }catch (e: FirebaseNetworkException){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Toast.makeText(requireActivity(),"Network Error, Check your network connection", Toast.LENGTH_LONG).show()
                    }
                }catch (e: FirebaseException){
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Toast.makeText(requireActivity(),"An internal error has occurred, try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    private fun checkUserAccessLevel() {
        try {
            var isCustomer = false
            var isSeller = false
            val isCustomerChecked: Boolean
            val isSellerChecked: Boolean
            val radioGroup = binding.rgrpLogin.checkedRadioButtonId
            val currentUser = SplashActivity.firebaseAuth.currentUser?.uid.toString()
            isCustomerChecked = binding.rbtnCustomer.id == radioGroup
            isSellerChecked = binding.rbtnSeller.id == radioGroup
            CoroutineScope(Dispatchers.IO).launch {
                val df1 = firestore.collection("customers").document(firebaseAuth.uid.toString())
                val df2 = firestore.collection("sellers").document(firebaseAuth.uid.toString())

                val documentCustomer = df1.get().await()
                if (documentCustomer.get("isCustomer").toString() == "1") {
                    isCustomer = true
                }

                val documentSeller = df2.get().await()
                if (documentSeller.get("isSeller").toString() == "1") {
                    isSeller = true
                }

                if (isCustomer && isCustomerChecked) {
                    Log.i("CLEAR","user: customer")
                    val dfc = firestore.collection("customers").document(currentUser)
                    val querySnapshot = dfc.get().await()
                    val storageRef = storage.reference
                    val imagesUrl = storageRef.child("customer/${firebaseAuth.uid.toString()}.jpg").downloadUrl.await()
                    val customerData = "${querySnapshot.getString("userName").toString()}\n${querySnapshot.getString("email").toString()}\n${querySnapshot.getString("phoneNo").toString()}\n$imagesUrl}"
                    val fos1 = requireContext().openFileOutput(Constants.customerFile, Context.MODE_PRIVATE)
                    fos1.write(customerData.toByteArray())
                    progressDialog.dismiss()
                    withContext(Dispatchers.Main) {
                        val fos2 = requireContext().openFileOutput(Constants.userType, Context.MODE_PRIVATE)
                        fos2.write("customer".toByteArray())
                        val intent = Intent(requireContext(), CustomerNavigationActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                        Toast.makeText(requireContext(), "Customer Signed in Successfully", Toast.LENGTH_LONG).show()
                    }
                } else if (isSeller && isSellerChecked) {
                    Log.i("CLEAR","user: seller")
                    val dfs = firestore.collection("sellers").document(currentUser)
                    var storeName = ""
                    var storeAddress = ""
                    var openingTime = ""
                    var closingTime = ""
                    var latitude = 0.0
                    var longitude = 0.0
                    val querySnapshot = dfs.collection("stores").get().await()
                    for (document in querySnapshot.documents) {
                        storeName = document.getString("storeName") as String
                        storeAddress = document.getString("storeAddress") as String
                        openingTime = document.getString("openingTime") as String
                        closingTime = document.getString("closingTime") as String
                        latitude = document.getDouble("latitude") as Double
                        longitude = document.getDouble("longitude") as Double
                            val editor = SplashActivity.sharedPreferences.edit()
                        editor.apply {
                            putString("storeName", storeName)
                        }.apply()
                    }
                    val querySnapshot2 = dfs.get().await()
                    val storageRef = storage.reference
                    val imagesUrl = storageRef.child("seller/${firebaseAuth.uid.toString()}.jpg").downloadUrl.await()
                    val sellerData = "${querySnapshot2.getString("userName").toString()}\n${querySnapshot2.getString("email").toString()}\n${querySnapshot2.getString("phoneNo").toString()}\n$storeName\n$storeAddress\n$imagesUrl\n$openingTime\n$closingTime\n$latitude\n$longitude"
                    val fos1 = requireContext().openFileOutput(Constants.sellerFile, Context.MODE_PRIVATE)
                    fos1.write(sellerData.toByteArray())

                    withContext(Dispatchers.Main) {
                        if (firebaseAuth.currentUser!!.isEmailVerified){
                            Constants.isVerified = true
                        }
                        val fos2 = requireContext().openFileOutput(Constants.userType, Context.MODE_PRIVATE)
                        fos2.write("seller".toByteArray())
                        progressDialog.dismiss()
                        val intent = Intent(requireContext(), SellerNavigationActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                        Toast.makeText(requireContext(), "Seller Signed in Successfully", Toast.LENGTH_LONG).show()
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Log.i("CLEAR", "no such user")
                        Toast.makeText(context, "No Such User Found", Toast.LENGTH_LONG).show()
                        firebaseAuth.signOut()
                    }
                }
            }
        }catch (e: FirebaseFirestoreException){
                progressDialog.dismiss()
                Toast.makeText(requireActivity(),e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLoggedInState(){
        val user = SplashActivity.firebaseAuth.currentUser
        if(user != null){
            Log.i("CLEAR","SIGNED IN")
            addPreference(args.language)
        }
    }

    private fun addPreference(lang: String){
        val editor = SplashActivity.sharedPreferences.edit()
        editor.apply {
            putString("Lang", lang)
        }.apply()
    }
}