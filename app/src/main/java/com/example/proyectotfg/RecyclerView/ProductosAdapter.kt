package com.example.proyectotfg.RecyclerView

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectotfg.DataBase.Producto
import com.example.proyectotfg.databinding.ItemProductoBinding

class ProductosAdapter(
    private var productos: List<Producto>,
    private val context: Context,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    fun updateProductos(newProductos: List<Producto>) {
        productos = newProductos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productos[position], onItemClick)
    }

    override fun getItemCount(): Int = productos.size

    class ViewHolder(private val binding: ItemProductoBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(p: Producto, onItemClick: (Producto) -> Unit) {
            Log.d("ADAPTER", "Mostrar: ${p.producto}")
            binding.nombre.text = p.producto
            binding.precio.text  = String.format("%.2f €", p.precio)
            binding.root.setOnClickListener { onItemClick(p) }
        }
    }
}
