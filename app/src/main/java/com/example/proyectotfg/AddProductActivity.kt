package com.example.proyectotfg

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectotfg.Api.AddAlmacenRequest
import com.example.proyectotfg.Api.AddAlmacenResponse
import com.example.proyectotfg.Api.AddProductoRequest
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.databinding.ActivityAddProductBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para añadir un nuevo producto al sistema.
 *
 * Maneja campos básicos de producto, generación dinámica de selectores de fechas de caducidad,
 * y llamadas a la API para insertar o actualizar registros de producto y almacén.
 */
class AddProductActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddProductActivity" // Tag para logs de depuración
    }

    private lateinit var binding: ActivityAddProductBinding

    // Opciones disponibles para los campos de tamaño y marca
    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinde", "Risis", "Matutano",
        "Jumpers", "Gatos"
    )

    // Lista dinámica que almacena las fechas de caducidad seleccionadas
    private val fechasList = mutableListOf<String>()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura NumberPicker para determinar cuántas fechas generar
        binding.npCantidad.minValue = 1
        binding.npCantidad.maxValue = 10
        binding.npCantidad.value = 1
        binding.npCantidad.setOnValueChangedListener { _, _, newVal ->
            generateDatePickers(newVal)
        }

        // Configuración de autocompletado para el campo de tamaño
        binding.etTamano.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, tamaños)
        )
        binding.etTamano.threshold = 1
        binding.etTamano.setOnClickListener { binding.etTamano.showDropDown() }
        binding.etTamano.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.etTamano.showDropDown() }

        // Configuración de autocompletado para el campo de marca
        binding.etMarca.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, marcas)
        )
        binding.etMarca.threshold = 1
        binding.etMarca.setOnClickListener { binding.etMarca.showDropDown() }
        binding.etMarca.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.etMarca.showDropDown() }

        // Mostrar u ocultar sección de fechas si se marca almacén
        binding.cbAlmacen.setOnCheckedChangeListener { _, checked ->
            binding.formAlmacen.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked && fechasList.isEmpty()) {
                // Genera un selector por defecto si no hay fechas
                binding.npCantidad.value = 1
                generateDatePickers(1)
            } else if (!checked) {
                // Limpia las vistas y la lista al desactivar
                binding.containerFechas.removeAllViews()
                fechasList.clear()
            }
        }

        // Botón para volver
        binding.buttonVolver.setOnClickListener { finish() }

        // Botón para validar y guardar producto
        binding.btnGuardar.setOnClickListener { checkOrCreateProducto() }
    }

    /**
     * Genera dinámicamente TextViews para la selección de fechas.
     * @param count Número de fechas a crear.
     */
    private fun generateDatePickers(count: Int) {
        binding.containerFechas.removeAllViews()
        fechasList.clear()
        repeat(count) { idx ->
            fechasList.add("") // placeholder para cada fecha
            val tv = TextView(this).apply {
                text = "Fecha ${idx + 1}: --/--/----"
                textSize = 16f
                setPadding(0, 16, 0, 16)
                setOnClickListener { showDatePicker(idx, this) }
            }
            binding.containerFechas.addView(tv)
        }
    }

    /**
     * Muestra un DatePickerDialog sin permitir fechas pasadas.
     * @param index Índice de la fecha en la lista.
     * @param tv    TextView donde actualizar la fecha.
     */
    private fun showDatePicker(index: Int, tv: TextView) {
        val hoy = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val sel = Calendar.getInstance().apply { set(year, month, day) }
                val fecha = sdf.format(sel.time)
                fechasList[index] = fecha
                tv.text = "Fecha ${index + 1}: $fecha"
            },
            hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = hoy.timeInMillis
        dialog.show()
    }

    /**
     * Paso 1: Valida campos básicos y realiza llamada a addProducto.php.
     * Inserta o detecta existencia del producto.
     */
    private fun checkOrCreateProducto() {
        val nombre = binding.etProducto.text.toString().trim()
        val tamano = binding.etTamano.text.toString().trim()
        val precio = binding.etPrecio.text.toString().toDoubleOrNull()
        val marca = binding.etMarca.text.toString().trim()
        if (nombre.isEmpty() || tamano.isEmpty() || precio == null || marca.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos básicos", Toast.LENGTH_SHORT).show()
            return
        }

        val dispoInt = if (binding.cbDisponibles.isChecked) 1 else 0
        val almacInt = if (binding.cbAlmacen.isChecked) 1 else 0
        val cantidad = if (almacInt == 1) binding.npCantidad.value else null

        if (almacInt == 1 && (cantidad == null || fechasList.size != cantidad || fechasList.any { it.isEmpty() })) {
            Toast.makeText(this, "Selecciona todas las fechas de caducidad", Toast.LENGTH_SHORT).show()
            return
        }

        val req = AddProductoRequest(
            producto = nombre,
            tamano = tamano,
            precio = precio,
            marca = marca,
            disponibles = dispoInt,
            almacen = almacInt,
            cantidad = cantidad,
            fechaCaducidad = if (almacInt == 1) fechasList.joinToString(",") else null
        )

        RetrofitClient.instance.addProducto(req)
            .enqueue(object : Callback<AddProductoResponse> {
                override fun onResponse(call: Call<AddProductoResponse>, response: Response<AddProductoResponse>) {
                    response.errorBody()?.let { err ->
                        Log.d(TAG, "addProducto errorBody raw: ${err.string()}")
                    }
                    if (!response.isSuccessful) {
                        Toast.makeText(this@AddProductActivity, "Error servidor producto: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                        return
                    }
                    val body = response.body() ?: run {
                        Toast.makeText(this@AddProductActivity, "Respuesta vacía al añadir producto", Toast.LENGTH_LONG).show()
                        return
                    }
                    when (body.status) {
                        "ok" -> {
                            Toast.makeText(this@AddProductActivity, "¡Producto añadido con éxito!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        "exists" -> body.existingId?.let { id ->
                            showConfirmAddAlmacen(id, cantidad!!, fechasList.joinToString(","))
                        } ?: Toast.makeText(this@AddProductActivity, "Error: ID existente no recuperado", Toast.LENGTH_LONG).show()
                        "error" -> Toast.makeText(this@AddProductActivity, "Error servidor: ${body.message ?: "desconocido"}", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(this@AddProductActivity, "Status inesperado: ${body.status}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                    Log.e(TAG, "addProducto onFailure", t)
                    Toast.makeText(this@AddProductActivity, "Fallo red añadir producto: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    /**
     * Paso 2: Diálogo para confirmar añadir fechas a producto existente.
     */
    private fun showConfirmAddAlmacen(productoId: Int, cantidad: Int, fechasCsv: String) {
        AlertDialog.Builder(this)
            .setTitle("El producto ya existe")
            .setMessage("¿Quieres añadir fechas de almacén para ese producto?")
            .setPositiveButton("Sí") { _, _ -> addOnlyAlmacen(productoId, cantidad, fechasCsv) }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Paso 3: Llamada a addAlmacen.php para agregar registros de almacén.
     */
    private fun addOnlyAlmacen(productoId: Int, cantidad: Int, fechasCsv: String) {
        val reqAlmacen = AddAlmacenRequest(
            producto_id = productoId,
            cantidad = cantidad,
            fechaCaducidad = fechasCsv
        )
        RetrofitClient.instance.addAlmacen(reqAlmacen)
            .enqueue(object : Callback<AddAlmacenResponse> {
                override fun onResponse(call: Call<AddAlmacenResponse>, response: Response<AddAlmacenResponse>) {
                    response.errorBody()?.let { err ->
                        Log.d(TAG, "addAlmacen errorBody raw: ${err.string()}")
                    }
                    if (!response.isSuccessful) {
                        Toast.makeText(this@AddProductActivity, "Error servidor almacén: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                        return
                    }
                    val body = response.body()
                    if (body?.status == "ok") {
                        Toast.makeText(this@AddProductActivity, "Fechas de almacén añadidas con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddProductActivity, "Error servidor: ${body?.message ?: "desconocido"}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<AddAlmacenResponse>, t: Throwable) {
                    Log.e(TAG, "addAlmacen onFailure", t)
                    Toast.makeText(this@AddProductActivity, "Fallo red/parsing almacén: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}