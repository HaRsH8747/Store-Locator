package com.example.storelocator.customerLogin.productList

import android.annotation.SuppressLint
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
import com.example.storelocator.databinding.FragmentCustomerProductListBinding
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.cacheAllProductsList
import com.example.storelocator.util.Constants.Companion.job2
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class CustomerProductListFragment : Fragment() {

    private lateinit var binding: FragmentCustomerProductListBinding
    private var productList = mutableListOf<AllDetailProduct>()
    private lateinit var listViewAdapter: CustomerLinearProductListAdapter
    private lateinit var gridViewAdapter: CustomerGridProductListAdapter
    private var searchedProductList = mutableListOf<AllDetailProduct>()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var count = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomerProductListBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseAuth = SplashActivity.firebaseAuth
        binding.productListSwipeRefresh.setOnRefreshListener {
            Log.i("CLEAR", "Refreshing...")
            fetchProductsFromFireStore()
        }
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (cacheAllProductsList.isNotEmpty() && !binding.productListSwipeRefresh.isRefreshing){
            binding.productListSwipeRefresh.isRefreshing = false
            productList.clear()
            productList.addAll(cacheAllProductsList)
            binding.linearProgressBar.hide()
            listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(productList))
            gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(productList))
            if (Constants.isLinearView){
                binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerview.adapter = listViewAdapter
            }else{
                binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
                binding.recyclerview.adapter = gridViewAdapter
            }
        }else{
            Log.i("CLEAR", "${cacheAllProductsList.size}\t${productList.size}")
            fetchProductsFromFireStore()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        count++
        requireActivity().invalidateOptionsMenu()
    }

    @ExperimentalStdlibApi
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (Constants.isLinearView){
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
                Constants.isLinearView = !Constants.isLinearView
                requireActivity().invalidateOptionsMenu()
                productList.clear()
                productList.addAll(cacheAllProductsList)
                binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(productList))
                binding.recyclerview.adapter = listViewAdapter
            }

            R.id.miProductGridView -> {
                Constants.isLinearView = !Constants.isLinearView
                requireActivity().invalidateOptionsMenu()
                productList.clear()
                productList.addAll(cacheAllProductsList)
                binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(productList))
                binding.recyclerview.adapter = gridViewAdapter
            }

            R.id.miRefresh -> {
                if (!binding.productListSwipeRefresh.isRefreshing){
                    binding.productListSwipeRefresh.isRefreshing = true
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
        job2 = CoroutineScope(Dispatchers.IO).launch {
            productList.clear()
            val querySnapshot1 = firestore.collection("sellers").get().await()
            for(productDocument1 in querySnapshot1.documents){
                //Get all sellers
                if (job2.isActive){
                    val querySnapshot2 = firestore.collection("sellers")
                            .document(productDocument1.id).collection("stores").get().await()

                    for (productDocument2 in querySnapshot2.documents){
                        //Get all stores of one user
                        val querySnapshot3 = firestore.collection("sellers")
                                .document(productDocument1.id).collection("stores")
                                .document(productDocument2.id).collection("products").get().await()

                        for (productDocument3 in querySnapshot3.documents){
                            //Get all products of one store
                            val oldStoreName = productDocument2.getString("oldStoreName") as String
                            val imagesUrl = storageRef.child("seller/${productDocument1.id}/$oldStoreName/products/${productDocument3.id}.jpg").downloadUrl.await()

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
                            productList.add(product)
                        }
                    }
                }
            }

            if (job2.isActive){
                cacheAllProductsList.clear()
                cacheAllProductsList.addAll(productList)

                withContext(Dispatchers.Main){
                    if (productList.isEmpty()){
                        binding.tvNoProduct.visibility = View.VISIBLE
                        binding.linearProgressBar.hide()
                        binding.productListSwipeRefresh.isRefreshing = false
                    }else{
                        binding.linearProgressBar.hide()
                        listViewAdapter = CustomerLinearProductListAdapter(sortByPopularity(productList))
                        gridViewAdapter = CustomerGridProductListAdapter(sortByPopularity(productList))
                        if (Constants.isLinearView){
                            binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                            binding.recyclerview.adapter = listViewAdapter
                        }else{
                            binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                            binding.recyclerview.adapter = gridViewAdapter
                        }
                        binding.productListSwipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }


    @ExperimentalStdlibApi
    private fun searchProducts(query: String){
        searchedProductList.clear()
        val pattern = Regex("^${query.lowercase()}")
        if (cacheAllProductsList.isNotEmpty()){
            for (product in cacheAllProductsList){
                if (pattern.containsMatchIn(product.productName.lowercase())){
                    searchedProductList.add(product)
                }
            }
        }
        productList.clear()
        productList.addAll(sortByPopularity(searchedProductList))
        if (Constants.isLinearView) {
            listViewAdapter = CustomerLinearProductListAdapter(productList)
            binding.recyclerview.adapter = listViewAdapter
        }else{
            gridViewAdapter = CustomerGridProductListAdapter(productList)
            binding.recyclerview.adapter = gridViewAdapter
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
        job2.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        job2.cancel()
    }

    private fun removeDuplicate(list: MutableList<AllDetailProduct>): MutableList<AllDetailProduct> {
        Log.i("CLEAR", "duplicate size: ${list.size}")
        for (i in 0 until list.size-1){
            for (j in i+1 until list.size){
                if (list[i] == list[j]){
                    list.removeAt(i)
                }
            }
        }
//        while (k <= list.size-1){
//            if (k+pointer < list.size && list[k] == list[k+pointer]){
//                list.removeAt(k+pointer)
//                pointer++
//            }
//            if (k+1 < list.size && list[k] == list[k+1]){
//                tempList.add(list[k])
//                k += 2
//            }else{
//                tempList.add(list[k])
//                k++
//            }
//        }
        return list
    }
}