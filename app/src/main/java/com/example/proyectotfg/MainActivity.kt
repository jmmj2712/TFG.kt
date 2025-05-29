package com.example.proyectotfg

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
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

    // --- Reemplazamos la lista simple por un mapa nombre→(precio, cantidad)
    private val resumen = mutableMapOf<String, Pair<Double, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        binding.recyclerViewProductos.layoutManager = GridLayoutManager(this, 3)
        adapter = ProductosAdapter(emptyList(), this) { producto ->
            Toast.makeText(this, "Producto: ${producto.producto}", Toast.LENGTH_SHORT).show()
            mostrarDetalle(producto)
            onProductoClicked(producto)
        }
        binding.recyclerViewProductos.adapter = adapter

        // Botones de marca
        binding.imageButtonRefresco.setOnClickListener { cargar("Cordero") }
        binding.imageButtonFini     .setOnClickListener { cargar("Fini") }
        binding.imageButtonChurruca .setOnClickListener { cargar("Churruca") }
        binding.imageButtonExtremena.setOnClickListener { cargar("Extremeñas") }
        binding.imageButtonFiesta   .setOnClickListener { cargar("Fiesta") }
        binding.imageButtonVidal    .setOnClickListener { cargar("Vidal") }
        binding.imageButtonGrefusa  .setOnClickListener { cargar("Grefusa") }
        binding.imageButtonTosfrit  .setOnClickListener { cargar("Tosfrit") }
        binding.imageButtonReyes    .setOnClickListener { cargar("Reyes") }
        binding.imageButtonKinder   .setOnClickListener { cargar("Kinder") }
        binding.imageButtonRisi     .setOnClickListener { cargar("Risi") }
        binding.imageButtonMatutano .setOnClickListener { cargar("Matutano") }
        binding.imageButtonJumpers  .setOnClickListener { cargar("Jumpers") }
        binding.imageButtonGato     .setOnClickListener { cargar("Gato") }

        // Botones de tamaño
        binding.buttonPequeno.setOnClickListener {
            currentSizeFilter = "Pequeño"
            aplicarFiltro()
        }
        binding.buttonGrande.setOnClickListener {
            currentSizeFilter = "Grande"
            aplicarFiltro()
        }

        // Botones inferiores
        binding.buttonNuevoPedido.setOnClickListener { limpiarResumen() }
        binding.buttonEditar     .setOnClickListener { startActivity(Intent(this, StoreActivity::class.java)) }
        binding.buttonAddProduct .setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }

        // Carga inicial
        cargar("Cordero")
    }

    /** Descarga productos (disponibles=1) filtrados por marca */
    fun cargar(marca: String?) {
        val m = marca ?: ""
        RetrofitClient.instance.getProductosPorMarca(m)
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

    /** Filtra localmente por tamaño */
    private fun aplicarFiltro() {
        val filtrados = currentProductos.filter {
            it.tamano.equals(currentSizeFilter, ignoreCase = true)
        }
        adapter.updateProductos(filtrados)
    }

    /** Muestra detalle textual */
    private fun mostrarDetalle(p: Producto) {
        binding.textViewProducto.text = p.producto
        binding.textViewTotal.text    = String.format("%.2f €", p.precio)
        binding.textViewProducto.visibility = View.VISIBLE
    }

    // --- NUEVAS FUNCIONES PARA EL RESUMEN CON CANTIDADES ---

    /** Añade/incrementa en el resumen y vuelve a renderizar */
    private fun onProductoClicked(p: Producto) {
        val key = p.producto
        val precioUnit = p.precio
        val cantidadActual = resumen[key]?.second ?: 0
        resumen[key!!] = precioUnit to (cantidadActual + 1)
        renderizarResumen()
    }

    /** Reconstruye todas las filas del resumen y recalcula total */
    private fun renderizarResumen() {
        binding.layoutResumen.removeAllViews()
        var total = 0.0

        resumen.forEach { (nombre, pair) ->
            val (precioUnit, qty) = pair
            total += precioUnit * qty

            // Fila horizontal
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Nombre (peso 2)
            row.addView(TextView(this).apply {
                text = nombre
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
            })

            // Botón "-" (decrementa)
            row.addView(ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_previous)
                setOnClickListener {
                    val nuevaQty = qty - 1
                    if (nuevaQty > 0) resumen[nombre] = precioUnit to nuevaQty
                    else resumen.remove(nombre)
                    renderizarResumen()
                }
            })

            // Cantidad
            row.addView(TextView(this).apply {
                text = qty.toString()
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            })

            // Botón "+" (incrementa)
            row.addView(ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_next)
                setOnClickListener {
                    resumen[nombre] = precioUnit to (qty + 1)
                    renderizarResumen()
                }
            })

            // Subtotal (peso 1)
            row.addView(TextView(this).apply {
                text = String.format("%.2f €", precioUnit * qty)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })

            binding.layoutResumen.addView(row)
        }

        // Total
        binding.textViewTotal.text = String.format("%.2f €", total)
    }

    /** Vacía el resumen por completo */
    private fun limpiarResumen() {
        resumen.clear()
        renderizarResumen()
        binding.textViewProducto.visibility = View.GONE
        Toast.makeText(this, "Nuevo pedido iniciado", Toast.LENGTH_SHORT).show()
    }
}
