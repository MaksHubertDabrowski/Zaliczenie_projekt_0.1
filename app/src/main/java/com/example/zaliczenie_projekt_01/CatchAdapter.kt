package com.example.zaliczenie_projekt_01

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // For loading images
import com.example.zaliczenie_projekt_01.data.db.Catch
import com.example.zaliczenie_projekt_01.databinding.ItemCatchBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchAdapter(
    private var catches: List<Catch>,
    private val onItemClick: (Catch) -> Unit
) : RecyclerView.Adapter<CatchAdapter.CatchViewHolder>() {

    class CatchViewHolder(private val binding: ItemCatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(catch: Catch, onItemClick: (Catch) -> Unit) {
            binding.textViewDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(catch.date))
            binding.textViewSpecies.text = catch.species ?: "Nieznany gatunek"

            // Load image if available
            catch.imagePath?.let { path ->
                val imageFile = File(path)
                if (imageFile.exists()) {
                    Glide.with(binding.imageViewThumbnail.context)
                        .load(imageFile)
                        .centerCrop()
                        .into(binding.imageViewThumbnail)
                } else {
                    // Set a placeholder or clear the image if file doesn't exist
                    binding.imageViewThumbnail.setImageResource(R.drawable.ic_launcher_background) // Placeholder
                }
            } ?: run {
                // No image path, set a placeholder
                binding.imageViewThumbnail.setImageResource(R.drawable.ic_launcher_background) // Placeholder
            }

            binding.root.setOnClickListener {
                onItemClick(catch)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatchViewHolder {
        val binding = ItemCatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatchViewHolder, position: Int) {
        holder.bind(catches[position], onItemClick)
    }

    override fun getItemCount(): Int = catches.size

    fun updateCatches(newCatches: List<Catch>) {
        catches = newCatches
        notifyDataSetChanged()
    }
}
