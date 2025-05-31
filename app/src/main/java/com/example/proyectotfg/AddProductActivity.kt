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

class AddProductActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddProductActivity"
    }

    private lateinit var binding: ActivityAddProductBinding

    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinder", "Risi", "Matutano",
        "Jumpers", "Gato"
    )

    // Lista para almacenar las fechas seleccionadas
    private val fechasList = mutableListOf<String>()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar NumberPicker y generar campos al cambiar
        binding.npCantidad.minValue = 1
        binding.npCantidad.maxValue = 10
        binding.npCantidad.value = 1
        binding.npCantidad.setOnValueChangedListener { _, _, newVal ->
            generateDatePickers(newVal)
        }

        // AutoComplete para tamaño
        binding.etTamano.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, tamaños)
        )
        binding.etTamano.threshold = 1
        binding.etTamano.setOnClickListener { binding.etTamano.showDropDown() }
        binding.etTamano.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etTamano.showDropDown()
        }

        // AutoComplete para marca
        binding.etMarca.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, marcas)
        )
        binding.etMarca.threshold = 1
        binding.etMarca.setOnClickListener { binding.etMarca.showDropDown() }
        binding.etMarca.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etMarca.showDropDown()
        }

        // Mostrar u ocultar el formulario de almacén
        binding.cbAlmacen.setOnCheckedChangeListener { _, checked ->
            binding.formAlmacen.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                // Si se ha marcado Almacenar, asegurar que haya al menos 1 fecha
                if (fechasList.isEmpty()) {
                    binding.npCantidad.value = 1
                    generateDatePickers(1)
                }
            } else {
                // Al desmarcar, limpiar fechas
                binding.containerFechas.removeAllViews()
                fechasList.clear()
            }
        }

        // Botón Volver
        binding.buttonVolver.setOnClickListener { finish() }

        // Botón Guardar → inicia flujo de comprobación y posible inserción
        binding.btnGuardar.setOnClickListener { checkOrCreateProducto() }
    }

    /** Crea tantos TextView para fechas como indique la cantidad */
    private fun generateDatePickers(count: Int) {
        binding.containerFechas.removeAllViews()
        fechasList.clear()
        repeat(count) { idx ->
            fechasList.add("")  // placeholder
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
     * Muestra un DatePicker para el campo de índice [index],
     * sin permitir seleccionar fechas previas a hoy.
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
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        )
        // Restringir el DatePicker para que no permita fechas anteriores a hoy
        dialog.datePicker.minDate = hoy.timeInMillis
        dialog.show()
    }

    /**
     * Paso 1: Validar campos básicos y llamar a addProducto.php para “comprobar existencia”
     * o insertar directamente si no existe.
     */
    private fun checkOrCreateProducto() {
        // 1) Validación campos básicos
        val nombre = binding.etProducto.text.toString().trim()
        val tamano = binding.etTamano.text.toString().trim()
        val precio = binding.etPrecio.text.toString().toDoubleOrNull()
        val marca = binding.etMarca.text.toString().trim()
        if (nombre.isEmpty() || tamano.isEmpty() || precio == null || marca.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos básicos", Toast.LENGTH_SHORT).show()
            return
        }

        // 2) Flags
        val dispoInt = if (binding.cbDisponibles.isChecked) 1 else 0
        val almacInt = if (binding.cbAlmacen.isChecked) 1 else 0
        val cantidad = if (almacInt == 1) binding.npCantidad.value else null

        // 3) Validación de fechas si almacInt == 1
        if (almacInt == 1) {
            if (cantidad == null || fechasList.size != cantidad || fechasList.any { it.isEmpty() }) {
                Toast.makeText(this, "Selecciona todas las fechas de caducidad", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4) Construir request para addProducto.php
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

        // 5) Llamada Retrofit a addProducto.php
        RetrofitClient.instance.addProducto(req)
            .enqueue(object : Callback<AddProductoResponse> {
                override fun onResponse(
                    call: Call<AddProductoResponse>,
                    response: Response<AddProductoResponse>
                ) {
                    // -- LOG RAW DE LA RESPUESTA (para DEBUG) --
                    response.errorBody()?.let { errBody ->
                        val rawError = try {
                            errBody.string()
                        } catch (e: Exception) {
                            "no se pudo leer errorBody"
                        }
                        Log.d(TAG, "addProducto() errorBody raw: $rawError")
                    }
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Error servidor producto: HTTP ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Respuesta vacía / no JSON al añadir producto",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    when (body.status) {
                        "ok" -> {
                            Toast.makeText(
                                this@AddProductActivity,
                                "¡Producto añadido con éxito!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        "exists" -> {
                            val existingId = body.existingId
                            if (existingId == null) {
                                Toast.makeText(
                                    this@AddProductActivity,
                                    "Error: no se recuperó el ID existente",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }
                            showConfirmAddAlmacen(existingId, cantidad!!, fechasList.joinToString(","))
                        }
                        "error" -> {
                            Toast.makeText(
                                this@AddProductActivity,
                                "Error del servidor: ${body.message ?: "desconocido"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this@AddProductActivity,
                                "Status inesperado: ${body.status}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                    Log.e(TAG, "addProducto() onFailure: ", t)
                    Toast.makeText(
                        this@AddProductActivity,
                        "Fallo de red al añadir producto: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    /**
     * Paso 2 (cuando addProducto responde “exists”):
     * Mostrar un diálogo para preguntar si se desea agregar solo el almacén.
     */
    private fun showConfirmAddAlmacen(productoId: Int, cantidad: Int, fechasCsv: String) {
        AlertDialog.Builder(this)
            .setTitle("El producto ya existe")
            .setMessage("Ya existe un producto con estos datos.\n¿Quieres añadir las fechas en almacén para ese producto?")
            .setPositiveButton("Sí") { _, _ ->
                addOnlyAlmacen(productoId, cantidad, fechasCsv)
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Paso 3 (si el usuario acepta “Sí”): llamar a addAlmacen.php
     */
    private fun addOnlyAlmacen(productoId: Int, cantidad: Int, fechasCsv: String) {
        val reqAlmacen = AddAlmacenRequest(
            producto_id = productoId,
            cantidad = cantidad,
            fechaCaducidad = fechasCsv
        )
        RetrofitClient.instance.addAlmacen(reqAlmacen)
            .enqueue(object : Callback<AddAlmacenResponse> {
                override fun onResponse(
                    call: Call<AddAlmacenResponse>,
                    response: Response<AddAlmacenResponse>
                ) {
                    // -- LOG RAW DE LA RESPUESTA DEL SERVIDOR (para DEBUG) --
                    response.errorBody()?.let { errBody ->
                        val rawError = try {
                            errBody.string()
                        } catch (e: Exception) {
                            "no se pudo leer errorBody"
                        }
                        Log.d(TAG, "addAlmacen() errorBody raw: $rawError")
                    }
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Error servidor almacén: HTTP ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Respuesta vacía / no JSON al añadir almacén",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    if (body.status == "ok") {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Fechas de almacén añadidas con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Error del servidor: ${body.message ?: "desconocido"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<AddAlmacenResponse>, t: Throwable) {
                    Log.e(TAG, "addAlmacen() onFailure: ", t)
                    Toast.makeText(
                        this@AddProductActivity,
                        "Fallo de red o parseo (almacén): ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
