package com.example.proyectotfg

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.DataBase.Almacen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AlmacenItemActions(private val context: Context) {

    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas  = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinder", "Risi", "Matutano",
        "Jumpers", "Gato"
    )

    /**
     * Sólo vincula acciones a productos en almacén (almacen == 1)
     */
    fun bindRowClick(rowView: View, item: Almacen, onUpdated: () -> Unit) {
        if (item.almacen != 1) return
        rowView.setOnClickListener {
            PopupMenu(context, rowView).apply {
                menu.add("Modificar")
                menu.add("Eliminar")
                setOnMenuItemClickListener { mi ->
                    when (mi.title.toString()) {
                        "Eliminar"  -> confirmDelete(item, onUpdated)
                        "Modificar" -> showEditDialog(item, onUpdated)
                    }
                    true
                }
            }.show()
        }
    }

    private fun confirmDelete(item: Almacen, onDeleted: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar “${item.producto}”")
            .setMessage("¿Seguro que quieres eliminar este producto?")
            .setPositiveButton("Sí") { _, _ ->
                RetrofitClient.instance.deleteProducto(item.id)
                    .enqueue(object : Callback<AddProductoResponse> {
                        override fun onResponse(
                            call: Call<AddProductoResponse>,
                            resp: Response<AddProductoResponse>
                        ) {
                            if (resp.isSuccessful && resp.body()?.status == "ok") {
                                Toast.makeText(context, "Eliminado con éxito", Toast.LENGTH_SHORT).show()
                                onDeleted()
                            } else {
                                Log.e("DeleteProducto", "HTTP ${resp.code()} - ${resp.errorBody()?.string()}")
                                Toast.makeText(context, "Error servidor: ${resp.code()}", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                            Log.e("DeleteProducto", "Fallo de red", t)
                            Toast.makeText(context, "Fallo de red: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(item: Almacen, onUpdated: () -> Unit) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_edit_item, null)

        // Referencias
        val etProd  = dialogView.findViewById<EditText>(R.id.etProducto)
        val etSize  = dialogView.findViewById<AutoCompleteTextView>(R.id.etTamano)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrecio)
        val etBrand = dialogView.findViewById<AutoCompleteTextView>(R.id.etMarca)
        val cbDisp  = dialogView.findViewById<CheckBox>(R.id.cbDisponibles)
        val cbAlm   = dialogView.findViewById<CheckBox>(R.id.cbAlmacen)
        val etCant  = dialogView.findViewById<EditText>(R.id.etCantidad)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDateFilter)
        val tvDate  = dialogView.findViewById<TextView>(R.id.tvFechaSeleccionada)

        // AutoComplete
        etSize.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, tamaños))
        etSize.threshold = 1
        etBrand.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, marcas))
        etBrand.threshold = 1

        // Precargar datos
        etProd.setText(item.producto)
        etSize.setText(item.tamano, false)
        etPrice.setText(item.precio.toString())
        etBrand.setText(item.marca, false)
        cbDisp.isChecked = item.disponibles == 1
        cbAlm .isChecked = true  // siempre en almacén
        etCant.setText(item.cantidad?.toString() ?: "")
        tvDate.text = item.fechaCaducidad ?: "--/--/----"

        // DatePicker
        btnDate.setOnClickListener {
            val parts = item.fechaCaducidad?.split("/")?.mapNotNull { it.toIntOrNull() }
            val cal = Calendar.getInstance().apply {
                if (parts != null && parts.size == 3) set(parts[2], parts[1]-1, parts[0])
            }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val text = String.format("%02d/%02d/%04d", day, month+1, year)
                    tvDate.text = text
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(context)
            .setTitle("Modificar “${item.producto}”")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Lectura nuevos valores
                val newProd  = etProd.text.toString().trim()
                val newSize  = etSize.text.toString().trim()
                val newPrice = etPrice.text.toString().toDoubleOrNull()
                val newBrand = etBrand.text.toString().trim()
                val newDisp  = if (cbDisp.isChecked) 1 else 0
                val newAlm   = 1
                val newCant  = etCant.text.toString().toIntOrNull()
                val newDate  = tvDate.text.toString().trim()

                // Validaciones
                if (newProd.isEmpty() || newSize.isEmpty() || newPrice == null || newBrand.isEmpty()) {
                    Toast.makeText(context, "Rellena campos básicos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newCant == null || newDate.isEmpty() || newDate == "--/--/----") {
                    Toast.makeText(context, "Cantidad y fecha requeridas", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Confirmar
                AlertDialog.Builder(context)
                    .setTitle("Confirmar cambios")
                    .setMessage("Guardar cambios en “$newProd”?")
                    .setPositiveButton("Sí") { _, _ ->
                        RetrofitClient.instance.editProducto(
                            item.id, newProd, newSize, newPrice,
                            newBrand, newDisp, newAlm, newCant, newDate
                        ).enqueue(object: Callback<AddProductoResponse> {
                            override fun onResponse(
                                call: Call<AddProductoResponse>,
                                resp: Response<AddProductoResponse>
                            ) {
                                if (resp.isSuccessful && resp.body()?.status == "ok") {
                                    Toast.makeText(context, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                                    onUpdated()
                                } else {
                                    Log.e("EditProducto", "HTTP ${resp.code()} - ${resp.errorBody()?.string()}")
                                    Toast.makeText(context, "Error servidor: ${resp.code()}", Toast.LENGTH_LONG).show()
                                }
                            }
                            override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                                Log.e("EditProducto", "Fallo de red", t)
                                Toast.makeText(context, "Fallo de red: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}