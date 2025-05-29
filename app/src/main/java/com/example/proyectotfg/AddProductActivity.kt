package com.example.proyectotfg

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectotfg.Api.AddProductoRequest
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.databinding.ActivityAddProductBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding

    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas  = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinder", "Risi", "Matutano",
        "Jumpers", "Gato"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar NumberPicker programáticamente
        binding.npCantidad.minValue = 1
        binding.npCantidad.maxValue = 1

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

        // Mostrar/Ocultar formulario de almacén
        binding.cbAlmacen.setOnCheckedChangeListener { _, checked ->
            binding.formAlmacen.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Selección de fecha
        binding.btnDateFilter.setOnClickListener { showDatePicker() }

        // Volver
        binding.buttonVolver.setOnClickListener { finish() }

        // Guardar
        binding.btnGuardar.setOnClickListener {
            // 1) Validación básica
            val nombre = binding.etProducto.text.toString().trim()
            val tamano = binding.etTamano.text.toString().trim()
            val precio = binding.etPrecio.text.toString().toDoubleOrNull()
            val marca  = binding.etMarca.text.toString().trim()
            if (nombre.isEmpty() || tamano.isEmpty() || precio == null || marca.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos básicos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2) Flags
            val dispoInt = if (binding.cbDisponibles.isChecked) 1 else 0
            val almacInt = if (binding.cbAlmacen.isChecked) 1 else 0

            // 3) Cantidad y fecha
            val cantidad = if (almacInt == 1) binding.npCantidad.value else null
            val fechaTxt = if (almacInt == 1) binding.tvFechaSeleccionada.text.toString().takeIf { it != "--/--/----" } else null

            if (almacInt == 1 && (cantidad == null || fechaTxt == null)) {
                Toast.makeText(this, "Selecciona cantidad y fecha de caducidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4) Construir request
            val req = AddProductoRequest(
                producto       = nombre,
                tamano         = tamano,
                precio         = precio,
                marca          = marca,
                disponibles    = dispoInt,
                almacen        = almacInt,
                cantidad       = cantidad,
                fechaCaducidad = fechaTxt
            )

            // 5) Llamada Retrofit
            RetrofitClient.instance.addProducto(req)
                .enqueue(object : Callback<AddProductoResponse> {
                    override fun onResponse(
                        call: Call<AddProductoResponse>,
                        response: Response<AddProductoResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status == "ok") {
                            Toast.makeText(
                                this@AddProductActivity,
                                "¡Producto añadido con éxito!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@AddProductActivity,
                                "Error: ${response.body()?.message ?: response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                        Toast.makeText(
                            this@AddProductActivity,
                            "Fallo de red: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    /** Muestra un DatePicker y guarda directamente en el TextView */
    private fun showDatePicker() {
        val hoy = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val sel = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvFechaSeleccionada.text = fmt.format(sel.time)
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
