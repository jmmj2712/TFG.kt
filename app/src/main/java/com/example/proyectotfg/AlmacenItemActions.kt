package com.example.proyectotfg

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.ConsumeResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.Model.Almacen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AlmacenItemActions(private val context: Context) {

    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinder", "Risi", "Matutano",
        "Jumpers", "Gato"
    )

    /**
     * Asocia un click listener a la fila (rowView). Cuando el usuario pulsa,
     * se mostrará un PopupMenu con opciones “Modificar”, “Eliminar” y “Consumir”.
     * Para cada acción, se utiliza el objeto `item` completo.
     * Al finalizar la acción, se invoca onUpdated().
     */
    fun bindRowClick(rowView: View, item: Almacen, onUpdated: () -> Unit) {
        // Sólo permitimos acciones si el ítem está en almacén
        if (item.almacen != 1) return

        rowView.setOnClickListener {
            PopupMenu(context, rowView).apply {
                menu.add("Modificar")
                menu.add("Eliminar")
                menu.add("Consumir")
                setOnMenuItemClickListener { mi ->
                    when (mi.title.toString()) {
                        "Eliminar"  -> confirmDelete(item, onUpdated)
                        "Modificar" -> showEditDialog(item, onUpdated)
                        "Consumir"  -> confirmConsume(item, onUpdated)
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
                                Log.e(
                                    "DeleteProducto",
                                    "HTTP ${resp.code()} - ${resp.errorBody()?.string()}"
                                )
                                Toast.makeText(
                                    context,
                                    "Error servidor: ${resp.code()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                            Log.e("DeleteProducto", "Fallo de red", t)
                            Toast.makeText(
                                context,
                                "Fallo de red: ${t.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmConsume(item: Almacen, onConsumed: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Consumir “${item.producto}”")
            .setMessage("¿Seguro que quieres consumir una unidad?")
            .setPositiveButton("Sí") { _, _ ->
                RetrofitClient.instance.consumeProduct(item.id)
                    .enqueue(object : Callback<ConsumeResponse> {
                        override fun onResponse(
                            call: Call<ConsumeResponse>,
                            resp: Response<ConsumeResponse>
                        ) {
                            val b = resp.body()
                            if (!resp.isSuccessful || b?.status != "ok") {
                                Toast.makeText(context, "Error al consumir", Toast.LENGTH_SHORT).show()
                                return
                            }
                            if (b.newCount == 0) {
                                Toast.makeText(context, "Producto agotado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Consumido, quedan ${b.newCount}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onConsumed()
                        }

                        override fun onFailure(call: Call<ConsumeResponse>, t: Throwable) {
                            Log.e("ConsumeProducto", "Fallo de red", t)
                            Toast.makeText(
                                context,
                                "Fallo de red: ${t.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(item: Almacen, onUpdated: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_item, null)

        // Mapeamos vistas (sin NumberPicker, porque ya lo quitamos del XML)
        val etProd  = dialogView.findViewById<TextView>(R.id.etProducto)
        val etSize  = dialogView.findViewById<AutoCompleteTextView>(R.id.etTamano)
        val etPrice = dialogView.findViewById<TextView>(R.id.etPrecio)
        val etBrand = dialogView.findViewById<AutoCompleteTextView>(R.id.etMarca)
        val cbDisp  = dialogView.findViewById<CheckBox>(R.id.cbDisponibles)
        val cbAlm   = dialogView.findViewById<CheckBox>(R.id.cbAlmacen)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDateFilter)
        val tvDate  = dialogView.findViewById<TextView>(R.id.tvFechaSeleccionada)

        // Configuramos los AutoComplete (tamaños y marcas)
        etSize.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, tamaños))
        etSize.threshold = 1
        etBrand.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, marcas))
        etBrand.threshold = 1

        // Cargamos valores actuales del item en el diálogo
        etProd.text  = item.producto
        etSize.setText(item.tamano, false)
        etPrice.text = item.precio.toString()
        etBrand.setText(item.marca, false)
        cbDisp.isChecked = item.disponibles == 1
        cbAlm.isChecked = (item.almacen == 1)
        tvDate.text = item.fechaCaducidad ?: "--/--/----"

        btnDate.setOnClickListener {
            val parts = item.fechaCaducidad?.split('/')?.mapNotNull { it.toIntOrNull() }
            val cal = Calendar.getInstance().apply {
                if (parts != null && parts.size == 3) set(parts[2], parts[1] - 1, parts[0])
            }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val text = String.format("%02d/%02d/%04d", day, month + 1, year)
                    tvDate.text = text
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(context)
            .setTitle("Modificar “${item.producto}”")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newProd  = etProd.text.toString().trim()
                val newSize  = etSize.text.toString().trim()
                val newPrice = etPrice.text.toString().toDoubleOrNull()
                val newBrand = etBrand.text.toString().trim()
                val newDisp  = if (cbDisp.isChecked) 1 else 0
                val newAlm   = if (cbAlm.isChecked) 1 else 0
                val newDate  = tvDate.text.toString().trim()

                // Validaciones básicas
                if (newProd.isEmpty() || newSize.isEmpty() || newPrice == null || newBrand.isEmpty()) {
                    Toast.makeText(context, "Rellena campos básicos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newDate.isBlank() || newDate == "--/--/----") {
                    Toast.makeText(context, "Fecha requerida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                AlertDialog.Builder(context)
                    .setTitle("Confirmar cambios")
                    .setMessage("Guardar cambios en “$newProd”?")
                    .setPositiveButton("Sí") { _, _ ->
                        // Para la “cantidad”, ahora simplemente enviamos la que ya tenía.
                        val unchangedCantidad = item.cantidad ?: 1

                        RetrofitClient.instance.editProducto(
                            item.id,
                            newProd,
                            newSize,
                            newPrice,
                            newBrand,
                            newDisp,
                            newAlm,
                            unchangedCantidad,  // no la dejamos modificar
                            newDate
                        ).enqueue(object : Callback<AddProductoResponse> {
                            override fun onResponse(
                                call: Call<AddProductoResponse>,
                                resp: Response<AddProductoResponse>
                            ) {
                                if (resp.isSuccessful && resp.body()?.status == "ok") {
                                    Toast.makeText(
                                        context,
                                        "Actualizado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onUpdated()
                                } else {
                                    Log.e(
                                        "EditProducto",
                                        "HTTP ${resp.code()} - ${resp.errorBody()?.string()}"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Error servidor: ${resp.code()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                                Log.e("EditProducto", "Fallo de red", t)
                                Toast.makeText(
                                    context,
                                    "Fallo de red: ${t.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
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
