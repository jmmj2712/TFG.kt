package com.example.proyectotfg.RecyclerView

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.Model.Producto
import com.example.proyectotfg.databinding.ItemProductoBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductosAdapter(
    private var productos: List<Producto>,
    private val context: Context,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    fun updateProductos(newProductos: List<Producto>) {
        productos = newProductos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemProductoBinding.inflate(LayoutInflater.from(context), parent, false))

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

            // Click en la tarjeta
            binding.root.setOnClickListener { onItemClick(p) }

            // Botón “Eliminar” → marcar no disponible
            binding.btnDelete.setOnClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Eliminar “${p.producto}”")
                    .setMessage("¿Quieres quitar este producto de disponibles?")
                    .setPositiveButton("Sí") { _, _ ->
                        RetrofitClient.instance
                            .available(p.id, 0)
                            .enqueue(object: Callback<AddProductoResponse> {
                                override fun onResponse(
                                    call: Call<AddProductoResponse>,
                                    response: Response<AddProductoResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.status == "ok") {
                                        android.widget.Toast.makeText(
                                            binding.root.context,
                                            "Producto marcado como no disponible",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        // Actualiza la lista recargando la marca actual
                                        (binding.root.context as? com.example.proyectotfg.MainActivity)
                                            ?.cargar(p.marca)
                                    } else {
                                        android.widget.Toast.makeText(
                                            binding.root.context,
                                            "Error: ${response.body()?.message ?: response.code()}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                                    android.widget.Toast.makeText(
                                        binding.root.context,
                                        "Fallo de red: ${t.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }
}
