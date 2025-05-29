package com.example.proyectotfg

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.DataBase.Almacen
import com.example.proyectotfg.Notification.NotificationWorker
import com.example.proyectotfg.databinding.ActivityStoreBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class StoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreBinding
    private lateinit var itemActions: AlmacenItemActions

    // Lista completa traída de la API
    private var allItems = listOf<Almacen>()
    // Filtros
    private var nameFilter: String? = null
    private var brandFilter: String? = null
    private var sizeFilter: String? = null
    private var toDate: Date? = null

    // Formateador para mostrar/parsear "dd/MM/yyyy"
    private val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemActions = AlmacenItemActions(this)

        // Inicializar textos
        binding.btnBrandFilter.text = "Todas"
        binding.btnSizeFilter.text  = "Tamaño"
        binding.btnDateFilter.text  = "Fecha"

        // Botón «Reiniciar filtros»
        binding.btnResetFilters.setOnClickListener {
            // Limpiar filtros
            nameFilter  = null
            brandFilter = null
            sizeFilter  = null
            toDate      = null

            // Reset UI
            binding.etFilterName.text?.clear()
            binding.btnBrandFilter.text = "Todas"
            binding.btnSizeFilter.text  = "Tamaño"
            binding.btnDateFilter.text  = "Fecha"

            // Mostrar todo sin filtros
            showInTable(allItems)
        }

        // Filtrar por nombre
        binding.btnApplyNameFilter.setOnClickListener {
            val txt = binding.etFilterName.text.toString().trim()
            nameFilter = txt.ifEmpty { null }
            applyFilters()
        }
        // Marca y tamaño
        binding.btnBrandFilter.setOnClickListener { showBrandMenu() }
        binding.btnSizeFilter.setOnClickListener { showSizeMenu() }
        // Fecha de corte
        binding.btnDateFilter.setOnClickListener { pickCutoffDate() }

        // Botón volver
        binding.buttonVolver.setOnClickListener { finish() }

        // Carga inicial
        loadAlmacen()
    }

    private fun loadAlmacen() {
        RetrofitClient.instance.getAlmacen().enqueue(object: Callback<List<Almacen>> {
            override fun onResponse(call: Call<List<Almacen>>, response: Response<List<Almacen>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@StoreActivity,
                        "Error ${response.code()} al cargar almacén",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                allItems = response.body().orEmpty()

                // Programar notificaciones
                allItems.forEach { item ->
                    NotificationWorker.programar(this@StoreActivity, item)
                }
                showInTable(allItems)
            }
            override fun onFailure(call: Call<List<Almacen>>, t: Throwable) {
                Toast.makeText(
                    this@StoreActivity,
                    "Fallo API: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun showBrandMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnBrandFilter)
        popup.menu.add("Todas")
        allItems.mapNotNull { it.marca }.distinct().sorted()
            .forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { menuItem ->
            brandFilter = menuItem.title.toString().takeIf { it != "Todas" }
            binding.btnBrandFilter.text = menuItem.title
            applyFilters()
            true
        }
        popup.show()
    }

    private fun showSizeMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnSizeFilter)
        popup.menu.add("Grande")
        popup.menu.add("Pequeño")
        popup.setOnMenuItemClickListener { menuItem ->
            sizeFilter = menuItem.title.toString()
            binding.btnSizeFilter.text = menuItem.title
            applyFilters()
            true
        }
        popup.show()
    }

    private fun pickCutoffDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                toDate = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }.time
                binding.btnDateFilter.text = displaySdf.format(toDate!!)
                applyFilters()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyFilters() {
        var filtered = allItems

        nameFilter?.let { nf ->
            filtered = filtered.filter {
                it.producto?.contains(nf, ignoreCase = true) == true
            }
        }
        brandFilter?.let { bf ->
            filtered = filtered.filter { it.marca == bf }
        }
        sizeFilter?.let { sf ->
            filtered = filtered.filter { it.tamano.equals(sf, ignoreCase = true) }
        }
        toDate?.let { cutoff ->
            filtered = filtered.filter { item ->
                item.fechaCaducidad
                    ?.let { displaySdf.parse(it)?.time }
                    ?.let { t -> t <= cutoff.time } ?: false
            }
        }

        if (filtered.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin resultados")
                .setMessage("No se han encontrado productos con esos filtros.")
                .setPositiveButton("OK", null)
                .show()
        }

        showInTable(filtered)
    }

    private fun showInTable(items: List<Almacen>) {
        val table = binding.tableLayoutAlmacen
        if (table.childCount > 1) table.removeViews(1, table.childCount - 1)

        items.forEach { item ->
            val row = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }
            row.addView(TextView(this).apply {
                setPadding(8,8,8,8)
                text = item.cantidad?.toString() ?: "—"
            })
            row.addView(TextView(this).apply {
                setPadding(8,8,8,8)
                text = item.producto ?: "—"
            })
            row.addView(TextView(this).apply {
                setPadding(8,8,8,8)
                text = item.marca ?: "—"
            })
            row.addView(TextView(this).apply {
                setPadding(8,8,8,8)
                text = item.tamano ?: "—"
            })
            row.addView(TextView(this).apply {
                setPadding(8,8,8,8)
                text = item.fechaCaducidad ?: "—"
            })

            itemActions.bindRowClick(row, item) { loadAlmacen() }
            table.addView(row)

            val divider = View(this).apply {
                val h = (resources.displayMetrics.density + 0.5f).roundToInt()
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, h
                )
                setBackgroundColor(Color.BLACK)
            }
            table.addView(divider)
        }
    }
}
