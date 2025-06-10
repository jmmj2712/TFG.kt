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

/**
 * Adaptador para mostrar una lista de productos en un RecyclerView.
 *
 * @param productos   Lista inicial de productos a mostrar.
 * @param context     Contexto de la aplicación, necesario para inflar vistas y mostrar diálogos.
 * @param onItemClick Callback invocado al pulsar sobre un elemento, recibiendo el producto.
 */
class ProductosAdapter(
    private var productos: List<Producto>,
    private val context: Context,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    /**
     * Actualiza la lista de productos y refresca la vista.
     *
     * @param newProductos Nueva lista de productos a mostrar.
     */
    fun updateProductos(newProductos: List<Producto>) {
        productos = newProductos
        notifyDataSetChanged()  // Refresca todo el RecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemProductoBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productos[position], onItemClick)
    }

    override fun getItemCount(): Int = productos.size

    /**
     * ViewHolder que representa cada tarjeta de producto.
     *
     * @param binding Enlace al layout de cada elemento (ItemProductoBinding).
     */
    class ViewHolder(private val binding: ItemProductoBinding)
        : RecyclerView.ViewHolder(binding.root) {

        /**
         * Enlaza los datos del producto con la vista y configura los eventos.
         *
         * @param p           Producto a mostrar.
         * @param onItemClick Callback para click sobre la tarjeta.
         */
        fun bind(p: Producto, onItemClick: (Producto) -> Unit) {
            Log.d("ADAPTER", "Mostrar: ${p.producto}")  // Debug: nombre del producto

            // Asigna nombre y precio formateado
            binding.nombre.text = p.producto
            binding.precio.text  = String.format("%.2f €", p.precio)

            // Click en toda la tarjeta
            binding.root.setOnClickListener { onItemClick(p) }

            // Botón “Eliminar”: marca el producto como no disponible (disponibles = 0)
            binding.btnDelete.setOnClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Eliminar “${p.producto}”")
                    .setMessage("¿Quieres quitar este producto de disponibles?")
                    .setPositiveButton("Sí") { _, _ ->
                        // Llama al endpoint 'available' para actualizar disponibilidad
                        RetrofitClient.instance
                            .available(p.id, 0)
                            .enqueue(object: Callback<AddProductoResponse> {
                                override fun onResponse(
                                    call: Call<AddProductoResponse>,
                                    response: Response<AddProductoResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.status == "ok") {
                                        // Notifica éxito al usuario
                                        android.widget.Toast.makeText(
                                            binding.root.context,
                                            "Producto marcado como no disponible",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        // Recarga la lista filtrada por la misma marca
                                        (binding.root.context as? com.example.proyectotfg.MainActivity)
                                            ?.cargar(p.marca)
                                    } else {
                                        // Muestra mensaje de error con detalle
                                        android.widget.Toast.makeText(
                                            binding.root.context,
                                            "Error: ${response.body()?.message ?: response.code()}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                                    // Error de red: informa al usuario
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
