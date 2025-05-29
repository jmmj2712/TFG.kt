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

    private var currentProductos: List<Producto> = emptyList()
    private var currentSizeFilter: String = "Pequeño"
    private var productosSeleccionados = mutableListOf<Pair<String, Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewProductos.layoutManager = GridLayoutManager(this, 3)
        adapter = ProductosAdapter(emptyList(), this) { producto ->
            Toast.makeText(this, "Producto: ${producto.producto}", Toast.LENGTH_SHORT).show()
            mostrarDetalle(producto)
            agregarAlResumen(producto.producto, producto.precio )
        }
        binding.recyclerViewProductos.adapter = adapter

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

        binding.buttonPequeno.setOnClickListener {
            currentSizeFilter = "Pequeño"
            aplicarFiltro()
        }
        binding.buttonGrande.setOnClickListener {
            currentSizeFilter = "Grande"
            aplicarFiltro()
        }

        binding.buttonNuevoPedido.setOnClickListener { nuevoPedido() }
        binding.buttonEditar     .setOnClickListener { startActivity(Intent(this, StoreActivity::class.java)) }
        binding.buttonAddProduct .setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }

        // Carga inicial
        cargar("Cordero")
    }

    /**
     * Ahora acepta String? y por dentro lo convierte a non-null.
     */
    fun cargar(marca: String?) {
        val marcaNonNull = marca ?: ""
        RetrofitClient.instance.getProductosPorMarca(marcaNonNull)
            .enqueue(object : Callback<List<Producto>> {
                override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Error HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }
                    currentProductos = response.body() ?: emptyList()
                    Log.d("API", "Recibidos: ${currentProductos.size}")
                    adapter.updateProductos(currentProductos)
                }
                override fun onFailure(call: Call<List<Producto>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun aplicarFiltro() {
        val filtrados = currentProductos.filter {
            it.tamano.equals(currentSizeFilter, ignoreCase = true)
        }
        adapter.updateProductos(filtrados)
    }

    private fun mostrarDetalle(p: Producto) {
        binding.textViewProducto.text = p.producto
        binding.textViewTotal.text    = String.format("%.2f €", p.precio ?: 0.0)
        binding.textViewProducto.visibility = View.VISIBLE
    }

    private fun agregarAlResumen(nombre: String, precio: Double) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply {
                text = nombre
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = String.format("%.2f €", precio)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
        }
        binding.layoutResumen.addView(fila)
        productosSeleccionados.add(nombre to precio)
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
