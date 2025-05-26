package com.example.proyectotfg

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.AutoCompleteTextView
import android.widget.PopupMenu
import android.widget.Toast
import com.example.proyectotfg.Api.AddProductoResponse
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.DataBase.Almacen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlmacenItemActions(private val context: Context) {

    private val tamaños = listOf("Pequeño", "Grande")
    private val marcas  = listOf(
        "Cordero", "Fini", "Churruca", "Extremeñas",
        "Fiesta", "Vidal", "Grefusa", "Tosfrit",
        "Reyes", "Kinder", "Risi", "Matutano",
        "Jumpers", "Gato"
    )

    fun bindRowClick(rowView: View, item: Almacen, onUpdated: () -> Unit) {
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
                                Toast.makeText(
                                    context,
                                    "Error: ${resp.body()?.message ?: resp.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                            Toast.makeText(context, "Fallo de red", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(item: Almacen, onUpdated: () -> Unit) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_edit_item, null)

        // References
        val etProd  = dialogView.findViewById<EditText>(R.id.etProducto)
        val etSize  = dialogView.findViewById<AutoCompleteTextView>(R.id.etTamano)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrecio)
        val etBrand = dialogView.findViewById<AutoCompleteTextView>(R.id.etMarca)
        val cbDisp  = dialogView.findViewById<CheckBox>(R.id.cbDisponibles)
        val cbAlm   = dialogView.findViewById<CheckBox>(R.id.cbAlmacen)
        val etCant  = dialogView.findViewById<EditText>(R.id.etCantidad)
        val etDate  = dialogView.findViewById<EditText>(R.id.etFechaCaducidad)

        // Configurar AutoComplete
        etSize.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, tamaños))
        etSize.threshold = 1
        etBrand.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, marcas))
        etBrand.threshold = 1

        // Precargar valores
        etProd.setText(item.producto)
        etSize.setText(item.tamano, false)
        etPrice.setText(item.precio.toString())
        etBrand.setText(item.marca, false)
        cbDisp.isChecked = item.disponibles == 1
        cbAlm .isChecked = item.almacen     == 1
        etCant.setText(item.cantidad?.toString() ?: "")
        etDate.setText(item.fechaCaducidad)

        AlertDialog.Builder(context)
            .setTitle("Modificar “${item.producto}”")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Leer nuevos valores
                val newProd  = etProd.text.toString().trim()
                val newSize  = etSize.text.toString().trim()
                val newPrice = etPrice.text.toString().toDoubleOrNull()
                val newBrand = etBrand.text.toString().trim()
                val newDisp  = if (cbDisp.isChecked) 1 else 0
                val newAlm   = if (cbAlm.isChecked ) 1 else 0
                val newCant  = etCant.text.toString().toIntOrNull()
                val newDate  = etDate.text.toString().trim()

                // Validar
                if (newProd.isEmpty() || newSize.isEmpty() || newPrice == null || newBrand.isEmpty()) {
                    Toast.makeText(context, "Rellena todos los campos básicos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newAlm == 1 && (newCant == null || newDate.isEmpty())) {
                    Toast.makeText(context, "Cantidad y fecha requeridas", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Confirmar y enviar update
                AlertDialog.Builder(context)
                    .setTitle("Confirmar cambios")
                    .setMessage("¿Deseas guardar cambios en “$newProd”?")
                    .setPositiveButton("Sí") { _, _ ->
                        RetrofitClient.instance.editProducto(
                            item.id,
                            newProd,
                            newSize,
                            newPrice,
                            newBrand,
                            newDisp,
                            newAlm,
                            newCant,
                            newDate
                        ).enqueue(object: Callback<AddProductoResponse> {
                            override fun onResponse(
                                call: Call<AddProductoResponse>,
                                resp: Response<AddProductoResponse>
                            ) {
                                if (resp.isSuccessful && resp.body()?.status == "ok") {
                                    Toast.makeText(context, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                                    onUpdated()
                                } else {
                                    Toast.makeText(context, "Error: ${resp.body()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<AddProductoResponse>, t: Throwable) {
                                Toast.makeText(context, "Fallo de red", Toast.LENGTH_SHORT).show()
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
