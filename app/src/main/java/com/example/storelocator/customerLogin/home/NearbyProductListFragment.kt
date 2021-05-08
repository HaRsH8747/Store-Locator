package com.example.storelocator.customerLogin.home

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.customerLogin.productList.AllDetailProduct
import com.example.storelocator.customerLogin.productList.CustomerGridProductListAdapter
import com.example.storelocator.customerLogin.productList.CustomerLinearProductListAdapter
import com.example.storelocator.databinding.FragmentNearbyProductListBinding
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.cacheNearbyProductsList
import com.example.storelocator.util.Constants.Companion.job3
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NearbyProductListFragment : Fragment() {

    private lateinit var binding: FragmentNearbyProductListBinding
    private var nearbyProductList = mutableListOf<AllDetailProduct>()
    private var searchedProductList = mutableListOf<AllDetailProduct>()
    private lateinit var listViewAdapter: CustomerLinearProductListAdapter
    private lateinit var gridViewAdapter: CustomerGridProductListAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var dataFetched = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentNearbyProductListBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseAuth = SplashActivity.firebaseAuth
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()
        binding.nearbyProductListSwipeRefresh.setOnRefreshListener {
            Log.i("CLEAR", "Refreshing...")
            fetchProductsFromFireStore()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (cacheNearbyProductsList.isNotEmpty() && !binding.nearbyProductListSwipeRefresh.isRefreshing){
            binding.nearbyProductListSwipeRefresh.isRefreshing = false
            nearbyProductList.clear()
            nearbyProductList.addAll(cacheNearbyProductsList)
            binding.nearbyLinearProgressBar.hide()
            listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(nearbyProductList))
            gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(nearbyProductList))
            if (Constants.isNearbyLinearView){
                Log.i("CLEAR","Resume Linear")
                binding.nearbyRecyclerview.layoutManager = LinearLayoutManager(requireContext())
                binding.nearbyRecyclerview.adapter = listViewAdapter
            }else{
                Log.i("CLEAR","Resume Grid")
                binding.nearbyRecyclerview.layoutManager = GridLayoutManager(requireContext(),3)
                binding.nearbyRecyclerview.adapter = gridViewAdapter
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        requireActivity().invalidateOptionsMenu()
    }

    @ExperimentalStdlibApi
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (Constants.isNearbyLinearView){
            inflater.inflate(R.menu.grid_view_menu, menu)
            val searchItem: MenuItem = menu.findItem(R.id.miSearch)
            val searchView = searchItem.actionView as SearchView
            val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = "Search Products"
            val searchPlateView: View = searchView.findViewById(androidx.appcompat.R.id.search_plate)
            searchPlateView.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        searchProducts(query)
                    }
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null) {
                        searchProducts(newText)
                    }
                    return false
                }
            })
        }else{
            inflater.inflate(R.menu.list_view_menu, menu)
            val searchItem: MenuItem = menu.findItem(R.id.miSearch)
            val searchView = searchItem.actionView as SearchView
            val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = "Search Products"
            val searchPlateView: View = searchView.findViewById(androidx.appcompat.R.id.search_plate)
            searchPlateView.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        searchProducts(query)
                    }

                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null) {
                        searchProducts(newText)
                    }
                    return false
                }
            })
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miProductListView -> {
                Constants.isNearbyLinearView = !Constants.isNearbyLinearView
                requireActivity().invalidateOptionsMenu()
                nearbyProductList.clear()
                nearbyProductList.addAll(Constants.cacheNearbyProductsList)
                binding.nearbyRecyclerview.layoutManager = LinearLayoutManager(requireContext())
                listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(nearbyProductList))
                binding.nearbyRecyclerview.adapter = listViewAdapter
            }

            R.id.miProductGridView -> {
                Constants.isNearbyLinearView = !Constants.isNearbyLinearView
                requireActivity().invalidateOptionsMenu()
                nearbyProductList.clear()
                nearbyProductList.addAll(cacheNearbyProductsList)
                binding.nearbyRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(nearbyProductList))
                binding.nearbyRecyclerview.adapter = gridViewAdapter
            }

            R.id.miRefresh -> {
                if (!binding.nearbyProductListSwipeRefresh.isRefreshing){
                    binding.nearbyProductListSwipeRefresh.isRefreshing = true
                    fetchProductsFromFireStore()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun fetchProductsFromFireStore(){
//        val userID = SplashActivity.firebaseAuth.currentUser?.uid.toString()
//        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        val storageRef = storage.reference
        job3 = CoroutineScope(Dispatchers.IO).launch {
            dataFetched = true
            nearbyProductList.clear()
            val querySnapshot1 = firestore.collection("sellers").get().await()
            for(productDocument1 in querySnapshot1.documents){
                //Get all sellers
                if (job3.isActive){
                    val querySnapshot2 = firestore.collection("sellers")
                        .document(productDocument1.id).collection("stores").get().await()

                    for (productDocument2 in querySnapshot2.documents){
                        //Get all stores of one user
                        val querySnapshot3 = firestore.collection("sellers")
                            .document(productDocument1.id).collection("stores")
                            .document(productDocument2.id).collection("products").get().await()

                        for (productDocument3 in querySnapshot3.documents){
                            //Get all products of one store
                            val imagesUrl = storageRef.child("seller/${productDocument1.id}/${productDocument2.id}/products/${productDocument3.id}.jpg").downloadUrl.await()

                            val product = AllDetailProduct(
                                productDocument2.id,
                                productDocument2.getString("storeAddress") as String,
                                LatLng(productDocument2.get("latitude") as Double, productDocument2.get("longitude") as Double),
                                productDocument2.getString("openingTime") as String,
                                productDocument2.getString("closingTime") as String,
                                productDocument3.getString("productName") as String,
                                (productDocument3.get("productPrice") as Double),
                                (productDocument3.get("discount") as Long).toInt(),
                                (productDocument3.get("extraDiscount") as Long).toInt(),
                                productDocument3.getString("extraOffer") as String,
                                (productDocument3.get("popularity") as Long).toInt(),
                                imagesUrl.toString()
                            )
                            //                        Log.i("CLEAR","product: $product")
                            nearbyProductList.add(product)
                        }
                    }
                }
            }

            if (job3.isActive){
                withContext(Dispatchers.Main){
                    val customerLocation = firestore.collection("customers").document(firebaseAuth.currentUser!!.uid).get().await()
                    val latitude = customerLocation.get("latitude") as Double
                    val longitude = customerLocation.get("longitude") as Double
                    Constants.latLng = LatLng(latitude, longitude)
                    cacheNearbyProductsList.clear()
                    if (Constants.latLng != LatLng(0.0,0.0)){
                        nearbyProductList = findNearbyProducts(nearbyProductList, Constants.latLng)
                        cacheNearbyProductsList.addAll(nearbyProductList)
                    }else{
                        nearbyProductList.addAll(nearbyProductList)
                        cacheNearbyProductsList.addAll(nearbyProductList)
                    }
                    if (nearbyProductList.isEmpty()){
                        binding.tvNearbyNoProduct.visibility = View.VISIBLE
                        binding.nearbyLinearProgressBar.hide()
                        binding.nearbyProductListSwipeRefresh.isRefreshing = false
                    }else{
                        binding.nearbyLinearProgressBar.hide()
                        listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(nearbyProductList))
                        gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(nearbyProductList))
                        if (Constants.isNearbyLinearView){
                            binding.nearbyRecyclerview.layoutManager = LinearLayoutManager(requireContext())
                            binding.nearbyRecyclerview.adapter = listViewAdapter
                        }else{
                            binding.nearbyRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                            binding.nearbyRecyclerview.adapter = gridViewAdapter
                        }
                        binding.nearbyProductListSwipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    private suspend fun findNearbyProducts(list: MutableList<AllDetailProduct>, latLng: LatLng): MutableList<AllDetailProduct> {
        try {
            val geoCoder = Geocoder(requireContext())
            val customerAddress: List<Address>  = geoCoder.getFromLocation(
                latLng.latitude, latLng.longitude, 1
            )
            for (i in 0 until list.size){
                val platLng = list[i].storeLatLng
                if (platLng != LatLng(0.0,0.0)){
                    val productAddress = geoCoder.getFromLocation(platLng.latitude,platLng.longitude,1)
                    if (productAddress[0].postalCode != customerAddress[0].postalCode){
                        list.removeAt(i)
                    }
                }
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Log.i("CLEAR","Unable to locate, Please try again!")
            }
        }
        return list
    }

    @ExperimentalStdlibApi
    private fun searchProducts(query: String){
        searchedProductList.clear()
        val pattern = Regex("^${query.lowercase()}")
        if (cacheNearbyProductsList.isNotEmpty()){
            for (product in cacheNearbyProductsList){
                if (pattern.containsMatchIn(product.productName.lowercase())){
                    searchedProductList.add(product)
                }
            }
        }
        nearbyProductList.clear()
        nearbyProductList.addAll(sortByPopularity(searchedProductList))
        if (Constants.isNearbyLinearView) {
            listViewAdapter = CustomerLinearProductListAdapter(nearbyProductList)
            binding.nearbyRecyclerview.adapter = listViewAdapter
        }else{
            gridViewAdapter = CustomerGridProductListAdapter(nearbyProductList)
            binding.nearbyRecyclerview.adapter = gridViewAdapter
        }
    }


    private fun sortByPopularity(list: MutableList<AllDetailProduct>): MutableList<AllDetailProduct>{
        var temp: AllDetailProduct
        for (i in 0 until (list.size-1)){
            for (j in 0 until (list.size-i-1)){
                if (list[j].popularity < list[j + 1].popularity){
                    temp = list[j]
                    list[j] = list[j + 1]
                    list[j + 1] = temp
                }
            }
        }
        return list
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (dataFetched){
            job3.cancel()
            dataFetched = false
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (dataFetched){
            job3.cancel()
            dataFetched = false
        }
    }
}