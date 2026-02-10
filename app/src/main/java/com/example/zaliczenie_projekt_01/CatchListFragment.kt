package com.example.zaliczenie_projekt_01

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.zaliczenie_projekt_01.data.db.AppDatabase
import com.example.zaliczenie_projekt_01.data.db.CatchDao
import com.example.zaliczenie_projekt_01.databinding.FragmentCatchListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CatchListFragment : Fragment() {

    private var _binding: FragmentCatchListBinding? = null
    private val binding get() = _binding!!
    private lateinit var catchDao: CatchDao
    private lateinit var catchAdapter: CatchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchDao = AppDatabase.getDatabase(requireContext()).catchDao()

        setupRecyclerView()
        loadCatches()
    }

    private fun setupRecyclerView() {
        catchAdapter = CatchAdapter(emptyList()) { catch ->
            // Handle item click - navigate to CatchDetailFragment
            val action = CatchListFragmentDirections.actionCatchListFragmentToCatchDetailFragment(catch.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewCatches.apply {
            layoutManager = GridLayoutManager(requireContext(), 2) // 2 kolumny
            adapter = catchAdapter
        }
    }

    private fun loadCatches() {
        CoroutineScope(Dispatchers.IO).launch {
            val catches = catchDao.getAllCatches()
            requireActivity().runOnUiThread {
                catchAdapter.updateCatches(catches)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}