package com.example.storelocator.login_register.register.seller.one

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.databinding.FragmentSellerRegisterBinding
import com.example.storelocator.login_register.register.seller.two.Seller
import com.example.storelocator.mainActivity.MainActivityViewModel

class SellerRegistrationFragment: Fragment() {

    private lateinit var binding: FragmentSellerRegisterBinding
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_seller_register, container,false)
        mainActivityViewModel.seller.observe(viewLifecycleOwner, { seller ->
            getValues(seller)
        })

        binding.btnBack.setOnClickListener {
            findNavController().navigate(SellerRegistrationFragmentDirections.actionSellerRegistrationFragmentToRegisterRoleSelector())
        }

        binding.btnNext.setOnClickListener {
            val userName = binding.etUsername.text.toString()
            val email = binding.etrEmail.text.toString()
            val password = binding.etrPassword.text.toString()
            val phoneNo = binding.etPhoneno.text.toString()
            if (isAllFieldsValid()){
                mainActivityViewModel.saveSeller(Seller(userName, email, password, phoneNo))
                findNavController().navigate(SellerRegistrationFragmentDirections.actionSellerRegistrationFragmentToSellerRegistrationTwoFragment())
            }
        }

        return binding.root
    }

    private fun isAllFieldsValid(): Boolean {
        val userName = binding.etUsername.text
        val email = binding.etrEmail.text
        val password = binding.etrPassword.text
        val phoneNo = binding.etPhoneno.text
        if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNo.isEmpty() || binding.etrPassword.text.length < 6 || (phoneNo.length<10 || phoneNo.length>10)){
            if (binding.etUsername.text.isEmpty()){
                binding.etUsername.error = "Enter your username"
            }
            if (binding.etrEmail.text.isEmpty()){
                binding.etrEmail.error = "Enter your email address"
            }
            if (binding.etrPassword.text.isEmpty()){
                binding.etrPassword.error = "Enter your password(At least 6 characters)"
            }
            if (binding.etrPassword.text.isNotEmpty() && binding.etrPassword.text.length < 6){
                binding.etrPassword.error = "Password must be of at least 6 characters"
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

    private fun getValues(seller: Seller) {
        binding.etUsername.setText(seller.userName)
        binding.etrEmail.setText(seller.email)
        binding.etrPassword.setText(seller.password)
        binding.etPhoneno.setText(seller.phoneNo)
    }
}