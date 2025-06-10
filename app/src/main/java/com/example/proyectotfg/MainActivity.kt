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
import com.example.proyectotfg.Model.Producto
import com.example.proyectotfg.RecyclerView.ProductosAdapter
import com.example.proyectotfg.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Actividad principal que muestra los productos disponibles y gestiona la selección
 * para crear un resumen de pedido.
 *
 * - Usa un RecyclerView en grid de 3 columnas para mostrar productos filtrados por marca/tamaño.
 * - Permite iniciar la creación de un nuevo pedido con incrementos/decrementos de cantidad.
 * - Ofrece navegación a añadir nuevo producto o ver la tienda.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProductosAdapter

    // Lista actual de productos descargados de la API
    private var currentProductos: List<Producto> = emptyList()

    // Filtro de tamaño aplicado localmente
    private var currentSizeFilter: String = "Pequeño"

    // Mapa resumen: nombre de producto → (precio unitario, cantidad seleccionada)
    private val resumen = mutableMapOf<String, Pair<Double, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura RecyclerView con GridLayout (3 columnas)
        binding.recyclerViewProductos.layoutManager = GridLayoutManager(this, 3)
        adapter = ProductosAdapter(emptyList(), this) { producto ->
            // Al pulsar un producto: mostrar detalle y añadir al resumen
            mostrarDetalle(producto)
            onProductoClicked(producto)
        }
        binding.recyclerViewProductos.adapter = adapter

        // Botones de marca: recargar lista según la marca pulsada
        binding.imageButtonRefresco.setOnClickListener { cargar("Cordero") }
        binding.imageButtonFini    .setOnClickListener { cargar("Fini") }
        binding.imageButtonChurruca.setOnClickListener { cargar("Churruca") }
        binding.imageButtonExtremena.setOnClickListener { cargar("Extremeñas") }
        binding.imageButtonFiesta  .setOnClickListener { cargar("Fiesta") }
        binding.imageButtonVidal   .setOnClickListener { cargar("Vidal") }
        binding.imageButtonGrefusa .setOnClickListener { cargar("Grefusa") }
        binding.imageButtonTosfrit .setOnClickListener { cargar("Tosfrit") }
        binding.imageButtonReyes   .setOnClickListener { cargar("Reyes") }
        binding.imageButtonKinder  .setOnClickListener { cargar("Kinde") }
        binding.imageButtonRisi    .setOnClickListener { cargar("Risis") }
        binding.imageButtonMatutano.setOnClickListener { cargar("Matutano") }
        binding.imageButtonJumpers .setOnClickListener { cargar("Jumpers") }
        binding.imageButtonGato    .setOnClickListener { cargar("Gatos") }

        // Botones de filtro por tamaño
        binding.buttonPequeno.setOnClickListener {
            currentSizeFilter = "Pequeño"
            aplicarFiltro()
        }
        binding.buttonGrande.setOnClickListener {
            currentSizeFilter = "Grande"
            aplicarFiltro()
        }

        // Botones inferiores: nuevo pedido, ver tienda y añadir producto
        binding.buttonNuevoPedido.setOnClickListener { limpiarResumen() }
        binding.buttonEditar     .setOnClickListener { startActivity(Intent(this, StoreActivity::class.java)) }
        binding.buttonAddProduct .setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }

        // Carga inicial de productos por defecto
        cargar("Cordero")
    }

    /**
     * Descarga de la API los productos disponibles (disponibles=1) para la [marca] dada
     * y actualiza el adaptador con la respuesta.
     */
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

    /** Filtra localmente [currentProductos] según el tamaño seleccionado y refresca el listado. */
    private fun aplicarFiltro() {
        val filtrados = currentProductos.filter {
            it.tamano.equals(currentSizeFilter, ignoreCase = true)
        }
        adapter.updateProductos(filtrados)
    }

    /**
     * Muestra en la parte superior el nombre y precio del producto [p] seleccionado.
     * Hace visible el panel de detalle si estaba oculto.
     */
    private fun mostrarDetalle(p: Producto) {
        binding.textViewProducto.text = p.producto
        binding.textViewTotal.text    = String.format("%.2f €", p.precio)
        binding.textViewProducto.visibility = View.VISIBLE
    }

    /**
     * Incrementa la cantidad de [p] en el resumen y refresca la vista de resumen.
     *
     * @param p Producto que se añade al pedido.
     */
    private fun onProductoClicked(p: Producto) {
        val key = p.producto
        val precioUnit = p.precio
        val cantidadActual = resumen[key]?.second ?: 0
        resumen[key] = precioUnit to (cantidadActual + 1)
        renderizarResumen()
    }

    /**
     * Reconstruye dinámicamente las filas del resumen en [binding.layoutResumen],
     * mostrando nombre, botones +/- para ajustar cantidad y subtotal.
     * Calcula y muestra el total acumulado.
     */
    private fun renderizarResumen() {
        binding.layoutResumen.removeAllViews()
        var total = 0.0

        resumen.forEach { (nombre, pair) ->
            val (precioUnit, qty) = pair
            total += precioUnit * qty

            // Fila horizontal contenedora
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Nombre del producto (peso 2)
            row.addView(TextView(this).apply {
                text = nombre
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
            })

            // Botón “-” para decrementar cantidad
            row.addView(ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_previous)
                setOnClickListener {
                    val nuevaQty = qty - 1
                    if (nuevaQty > 0) resumen[nombre] = precioUnit to nuevaQty
                    else resumen.remove(nombre)
                    renderizarResumen()
                }
            })

            // Texto con cantidad actual
            row.addView(TextView(this).apply {
                text = qty.toString()
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            })

            // Botón “+” para incrementar cantidad
            row.addView(ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_next)
                setOnClickListener {
                    resumen[nombre] = precioUnit to (qty + 1)
                    renderizarResumen()
                }
            })

            // Subtotal para este producto (peso 1)
            row.addView(TextView(this).apply {
                text = String.format("%.2f €", precioUnit * qty)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })

            binding.layoutResumen.addView(row)
        }

        // Muestra el total acumulado en el TextView de total
        binding.textViewTotal.text = String.format("%.2f €", total)
    }

    /** Limpia completamente el resumen y oculta el detalle de producto. */
    private fun limpiarResumen() {
        resumen.clear()
        renderizarResumen()
        binding.textViewProducto.visibility = View.GONE
        Toast.makeText(this, "Nuevo pedido iniciado", Toast.LENGTH_SHORT).show()
    }
}
