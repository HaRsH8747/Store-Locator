package com.example.storelocator.login_register.languageselector

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storelocator.databinding.FragmentLanguageSelectorBinding
import java.util.*


class LanguageSelector : Fragment() {

    private lateinit var binding: FragmentLanguageSelectorBinding

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentLanguageSelectorBinding.inflate(
                inflater,
                container,
                false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgbtnEnglish.setOnClickListener {
                setLanguage("en")
                findNavController().navigate(LanguageSelectorDirections.actionLanguageSelectorToStoreLogin("en"))
        }

        binding.imgbtnHindi.setOnClickListener {
                setLanguage("hi")
                findNavController().navigate(LanguageSelectorDirections.actionLanguageSelectorToStoreLogin("hi"))
        }

        binding.imgbtnGujarati.setOnClickListener {
                setLanguage("gu")
                findNavController().navigate(LanguageSelectorDirections.actionLanguageSelectorToStoreLogin("gu"))
        }
    }

    private fun setLanguage(lang: String){
        val res: Resources = requireContext().resources
        val conf: Configuration = res.configuration
        conf.setLocale(Locale(lang.toLowerCase()))
        res.updateConfiguration(conf, res.displayMetrics)
        Log.i("CLEAR", "differ Language changed to $lang")
    }
}