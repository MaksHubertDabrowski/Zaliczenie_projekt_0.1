package com.example.zaliczenie_projekt_01

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.zaliczenie_projekt_01.data.db.AppDatabase
import com.example.zaliczenie_projekt_01.data.db.CatchDao
import com.example.zaliczenie_projekt_01.databinding.FragmentCatchDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchDetailFragment : Fragment() {

    private var _binding: FragmentCatchDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var catchDao: CatchDao
    private val args: CatchDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchDao = AppDatabase.getDatabase(requireContext()).catchDao()

        val catchId = args.catchId

        CoroutineScope(Dispatchers.IO).launch {
            val catch = catchDao.getCatchById(catchId)
            requireActivity().runOnUiThread {
                catch?.let { displayCatchDetails(it) }
            }
        }
    }

    private fun displayCatchDetails(catch: com.example.zaliczenie_projekt_01.data.db.Catch) {
        binding.textViewDetailDate.text = "Data: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(catch.date))}"
        binding.textViewDetailSpecies.text = "Gatunek: ${catch.species ?: "Nieznany gatunek"}"
        binding.textViewDetailWeight.text = "Waga: ${catch.weight?.toString() ?: "N/A"}g"

        catch.imagePath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                Glide.with(binding.imageViewCatchDetail.context)
                    .load(imageFile)
                    .centerCrop()
                    .into(binding.imageViewCatchDetail)
            } else {
                binding.imageViewCatchDetail.setImageResource(R.drawable.ic_launcher_background) // Placeholder
            }
        } ?: run {
            binding.imageViewCatchDetail.setImageResource(R.drawable.ic_launcher_background) // Placeholder
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}