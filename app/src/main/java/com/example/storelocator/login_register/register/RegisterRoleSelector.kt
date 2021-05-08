package com.example.storelocator.login_register.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.R
import com.example.storelocator.databinding.FragmentRegisterRoleSelectorBinding


class RegisterRoleSelector : Fragment() {

    private lateinit var binding: FragmentRegisterRoleSelectorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register_role_selector, container, false)
        binding.btnCustomer.setOnClickListener {
            roleOfCustomer()
        }

        binding.btnSeller.setOnClickListener {
            roleSeller()
        }

//        binding.btnBack.setOnClickListener {
//            findNavController().navigate(RegisterRoleSelectorDirections.actionRegisterRoleSelectorToStoreLogin(language))
//        }

        return binding.root
    }

    private fun roleOfCustomer(){
        findNavController().navigate(RegisterRoleSelectorDirections.actionRegisterRoleSelectorToCustomerRegisterFragment())
    }

    private fun roleSeller(){
        findNavController().navigate(RegisterRoleSelectorDirections.actionRegisterRoleSelectorToSellerRegisterFragment())
    }
}