package com.example.storelocator.sellerLogin.ui.productslist

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storelocator.R
import com.example.storelocator.SplashActivity
import com.example.storelocator.databinding.FragmentSellerProductListBinding
import com.example.storelocator.sellerLogin.ui.home.editproduct.SelectProductAdapter
import com.example.storelocator.util.Constants
import com.example.storelocator.util.Constants.Companion.cacheProductsList
import com.example.storelocator.util.Constants.Companion.job5
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class SellerProductListFragment : Fragment() {

    private var productList = mutableListOf<Product>()
    private var searchedProductList = mutableListOf<Product>()
    private lateinit var binding: FragmentSellerProductListBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var listViewAdapter: ProductListAdapter
    private lateinit var gridViewAdapter: SelectProductAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        binding = FragmentSellerProductListBinding.inflate(layoutInflater)
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()
        firebaseAuth = SplashActivity.firebaseAuth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        binding.sellerProductListSwipeRefresh.setOnRefreshListener {
            fetchProductsFromFireStore()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (cacheProductsList.isEmpty()){
            fetchProductsFromFireStore()
        }else{
            binding.linearProgressBar.hide()
            productList.clear()
            productList.addAll(cacheProductsList)
            if (Constants.isLinearView){
                listViewAdapter = ProductListAdapter(productList)
                binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerview.adapter = listViewAdapter
            }else{
                gridViewAdapter = SelectProductAdapter(productList)
                binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                binding.recyclerview.adapter = gridViewAdapter
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        requireActivity().invalidateOptionsMenu()
    }

    @ExperimentalStdlibApi
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (Constants.isLinearView){
            inflater.inflate(R.menu.grid_view_menu,menu)
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
            inflater.inflate(R.menu.list_view_menu,menu)
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
//            val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
//            searchView.setSearchableInfo(searchManager.getSearchableInfo())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miProductListView ->{
                Constants.isLinearView = !Constants.isLinearView
                requireActivity().invalidateOptionsMenu()
                productList.clear()
                productList.addAll(cacheProductsList)
                binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                listViewAdapter = ProductListAdapter(productList)
                binding.recyclerview.adapter = listViewAdapter
            }

            R.id.miProductGridView ->{
                Constants.isLinearView = !Constants.isLinearView
                requireActivity().invalidateOptionsMenu()
                productList.clear()
                productList.addAll(cacheProductsList)
                binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
                gridViewAdapter = SelectProductAdapter(productList)
                binding.recyclerview.adapter = gridViewAdapter
            }

            R.id.miRefresh -> {
                if (!binding.sellerProductListSwipeRefresh.isRefreshing){
                    binding.sellerProductListSwipeRefresh.isRefreshing = true
                    fetchProductsFromFireStore()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun fetchProductsFromFireStore(){
        val userID = SplashActivity.firebaseAuth.currentUser?.uid.toString()
        val storeName = SplashActivity.sharedPreferences.getString("storeName",null)
        val storageRef = storage.reference
        job5 = CoroutineScope(Dispatchers.IO).launch {
            productList.clear()
            val querySnapshot1 = firestore.collection("sellers").document(userID)
                    .collection("stores").document(storeName.toString()).get().await()
            val oldStoreName = querySnapshot1.getString("oldStoreName") as String
            val querySnapshot2 = firestore.collection("sellers").document(userID)
                .collection("stores").document(storeName.toString()).collection("products").orderBy("productName",Query.Direction.ASCENDING).get().await()
                    for (productDocument in querySnapshot2.documents) {
                        val imagesUrl = storageRef.child("seller/$userID/$oldStoreName/products/${productDocument.getString("productName") as String}.jpg").downloadUrl.await()
                        val product = Product(
                            productDocument.getString("productName") as String,
                            (productDocument.get("productPrice") as Double),
                            (productDocument.get("discount") as Long).toInt(),
                            (productDocument.get("extraDiscount") as Long).toInt(),
                            productDocument.getString("extraOffer") as String,
                            imagesUrl.toString(),
                            (productDocument.get("popularity") as Long).toInt(),
                        )
                        productList.add(product)
                    }

            if (job5.isActive){

                cacheProductsList.clear()
                cacheProductsList.addAll(productList)

                withContext(Dispatchers.Main){
                    if (productList.isEmpty()){
                        binding.tvNoProduct.visibility = View.VISIBLE
                        binding.linearProgressBar.hide()
                        binding.sellerProductListSwipeRefresh.isRefreshing = false
                    }else{
                        binding.linearProgressBar.hide()
                        listViewAdapter = ProductListAdapter(productList)
                        gridViewAdapter = SelectProductAdapter(productList)
                        if (Constants.isLinearView){
                            binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
                            binding.recyclerview.adapter = listViewAdapter
                        }else{
                            binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
                            binding.recyclerview.adapter = gridViewAdapter
                        }
                        binding.sellerProductListSwipeRefresh.isRefreshing = false
                    }
                }
            }

        }
    }

    @ExperimentalStdlibApi
    private fun searchProducts(query: String){
        searchedProductList.clear()
        val pattern = Regex("^${query.lowercase()}")
        if (productList.isNotEmpty()){
            for (product in productList){
                if (pattern.containsMatchIn(product.productName.lowercase())){
                    searchedProductList.add(product)
                }
            }
        }
        if (Constants.isLinearView) {
            listViewAdapter = ProductListAdapter(searchedProductList)
            binding.recyclerview.adapter = listViewAdapter
        }else{
            gridViewAdapter = SelectProductAdapter(searchedProductList)
            binding.recyclerview.adapter = gridViewAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestore.clearPersistence()
        job5.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Constants.product = Product("",0.0,0,0,"","",0)
        job5.cancel()
    }
}