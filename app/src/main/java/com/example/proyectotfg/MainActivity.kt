package com.example.proyectotfg

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.DataBase.Producto
import com.example.proyectotfg.RecyclerView.ProductosAdapter
import com.example.proyectotfg.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProductosAdapter

    // Lista completa recibida de la API
    private var currentProductos: List<Producto> = emptyList()
    // Filtro activo de tamaño
    private var currentSizeFilter: String = "Pequeño"

    // Para el resumen de compra
    private var productosSeleccionados = mutableListOf<Pair<String, Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView (3 columnas)
        binding.recyclerViewProductos.layoutManager = GridLayoutManager(this, 3)
        adapter = ProductosAdapter(emptyList(), this) { producto ->
            Toast.makeText(this, "Producto: ${producto.producto}", Toast.LENGTH_SHORT).show()
            mostrarDetalle(producto)
            agregarAlResumen(producto.producto, producto.precio)
        }
        binding.recyclerViewProductos.adapter = adapter

        // Botones de marca
        binding.imageButtonRefresco.setOnClickListener { cargar("Cordero") }
        binding.imageButtonFini      .setOnClickListener { cargar("Fini") }
        binding.imageButtonChurruca  .setOnClickListener { cargar("Churruca") }
        binding.imageButtonExtremena .setOnClickListener { cargar("Extremeñas") }
        binding.imageButtonFiesta    .setOnClickListener { cargar("Fiesta") }
        binding.imageButtonVidal     .setOnClickListener { cargar("Vidal") }
        binding.imageButtonGrefusa   .setOnClickListener { cargar("Grefusa") }
        binding.imageButtonTosfrit   .setOnClickListener { cargar("Tosfrit") }
        binding.imageButtonReyes     .setOnClickListener { cargar("Reyes") }
        binding.imageButtonKinder    .setOnClickListener { cargar("Kinde") }
        binding.imageButtonRisi      .setOnClickListener { cargar("Risis") }
        binding.imageButtonMatutano  .setOnClickListener { cargar("Matutano") }
        binding.imageButtonJumpers   .setOnClickListener { cargar("Jumpers") }
        binding.imageButtonGato      .setOnClickListener { cargar("Gatos") }

        // Botones de tamaño
        binding.buttonPequeno.setOnClickListener {
            currentSizeFilter = "Pequeño"
            aplicarFiltro()
        }
        binding.buttonGrande.setOnClickListener {
            currentSizeFilter = "Grande"
            aplicarFiltro()
        }

        // Nuevo pedido
        binding.buttonNuevoPedido.setOnClickListener { nuevoPedido() }

        // Ir a editar stock
        binding.buttonEditar.setOnClickListener {
            startActivity(Intent(this, StoreActivity::class.java))
        }

        // Ir a añadir producto
        binding.buttonAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }
    }

    private fun mostrarDetalle(p: Producto) {
        binding.textViewProducto.text = p.producto
        binding.textViewTotal.text = String.format("%.2f €", p.precio)
        binding.textViewProducto.visibility = View.VISIBLE
    }

    private fun cargar(marca: String) {
        RetrofitClient.instance.getProductosPorMarca(marca)
            .enqueue(object : Callback<List<Producto>> {
                override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Error HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }
                    currentProductos = response.body() ?: emptyList()
                    Log.d("API", "Totales recibidos: ${currentProductos.size}")

                    // **1) Mostrar TODO** para verificar que funciona
                    adapter.updateProductos(currentProductos)
                    Log.d("API", "Adapter trazó ${currentProductos.size} ítems")

                    // Si quieres el filtro, en lugar de la línea de arriba, usas:
                    // aplicarFiltro()
                }

                override fun onFailure(call: Call<List<Producto>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Si descomentas en cargar(), aplica el filtro de tamaño
    private fun aplicarFiltro() {
        val filtrados = currentProductos.filter {
            it.tamano.equals(currentSizeFilter, ignoreCase = true)
        }
        adapter.updateProductos(filtrados)
        Log.d("API", "Adapter filtrado trazó ${filtrados.size} ítems de tamaño $currentSizeFilter")
    }

    private fun agregarAlResumen(nombre: String, precio: Double) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val tvN = TextView(context).apply {
                text = nombre
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvP = TextView(context).apply {
                text = String.format("%.2f €", precio)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            addView(tvN); addView(tvP)
        }
        binding.layoutResumen.addView(fila)
        productosSeleccionados.add(Pair(nombre, precio))
        actualizarTotal()
    }

    private fun actualizarTotal() {
        val total = productosSeleccionados.sumOf { it.second }
        binding.textViewTotal.text = String.format("%.2f €", total)
    }

    private fun nuevoPedido() {
        binding.layoutResumen.removeAllViews()
        productosSeleccionados.clear()
        binding.textViewTotal.text = "0,00 €"
        binding.textViewProducto.visibility = View.GONE
        Toast.makeText(this, "Nuevo pedido iniciado", Toast.LENGTH_SHORT).show()
    }
}
