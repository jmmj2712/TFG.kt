package com.example.proyectotfg

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectotfg.Api.RetrofitClient
import com.example.proyectotfg.Model.Almacen
import com.example.proyectotfg.Notification.NotificationWorker
import com.example.proyectotfg.databinding.ActivityStoreBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class StoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreBinding
    private var allItems = listOf<Almacen>()
    private val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private data class GroupItem(
        val id: Int,
        val product: String,
        val brand: String,
        val size: String,
        var count: Int,
        var nextDate: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBrandFilter.text = "Todas"
        binding.btnSizeFilter.text = "Tamaño"
        binding.btnDateFilter.text = "Fecha"

        binding.buttonVolver.setOnClickListener { finish() }
        binding.btnResetFilters.setOnClickListener { resetFilters() }
        binding.btnApplyNameFilter.setOnClickListener { applyFilters() }
        binding.btnBrandFilter.setOnClickListener { showBrandMenu() }
        binding.btnSizeFilter.setOnClickListener { showSizeMenu() }
        binding.btnDateFilter.setOnClickListener { pickCutoffDate() }

        loadAlmacen()
    }

    private fun loadAlmacen() {
        RetrofitClient.instance.getAlmacen().enqueue(object : Callback<List<Almacen>> {
            override fun onResponse(call: Call<List<Almacen>>, resp: Response<List<Almacen>>) {
                if (!resp.isSuccessful) {
                    Toast.makeText(this@StoreActivity, "Error ${resp.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                allItems = resp.body().orEmpty()
                allItems.forEach { NotificationWorker.programar(this@StoreActivity, it) }
                showInTable(allItems)
            }

            override fun onFailure(call: Call<List<Almacen>>, t: Throwable) {
                Toast.makeText(this@StoreActivity, "Fallo API: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetFilters() {
        binding.etFilterName.text?.clear()
        binding.btnBrandFilter.text = "Todas"
        binding.btnSizeFilter.text = "Tamaño"
        binding.btnDateFilter.text = "Fecha"
        showInTable(allItems)
    }

    private fun showBrandMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnBrandFilter)
        popup.menu.add("Todas")
        allItems.mapNotNull { it.marca }.distinct().sorted()
            .forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { mi ->
            binding.btnBrandFilter.text = mi.title
            applyFilters()
            true
        }
        popup.show()
    }

    private fun showSizeMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnSizeFilter)
        popup.menu.add("Grande")
        popup.menu.add("Pequeño")
        popup.setOnMenuItemClickListener { mi ->
            binding.btnSizeFilter.text = mi.title
            applyFilters()
            true
        }
        popup.show()
    }

    private fun pickCutoffDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                binding.btnDateFilter.text = displaySdf.format(
                    Calendar.getInstance().apply { set(y, m, d) }.time
                )
                applyFilters()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyFilters() {
        var filtered = allItems
        val name = binding.etFilterName.text.toString().trim()
        if (name.isNotEmpty()) filtered = filtered.filter { it.producto.contains(name, true) }
        if (binding.btnBrandFilter.text != "Todas") {
            filtered = filtered.filter { it.marca == binding.btnBrandFilter.text }
        }
        if (binding.btnSizeFilter.text != "Tamaño") {
            filtered = filtered.filter { it.tamano.equals(binding.btnSizeFilter.text.toString(), true) }
        }
        if (binding.btnDateFilter.text != "Fecha") {
            val cutoff = displaySdf.parse(binding.btnDateFilter.text.toString())!!
            filtered = filtered.filter {
                it.fechaCaducidad?.let { f -> displaySdf.parse(f)!!.time <= cutoff.time } == true
            }
        }
        if (filtered.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin resultados")
                .setMessage("No se han encontrado productos.")
                .setPositiveButton("OK", null)
                .show()
        }
        showInTable(filtered)
    }

    private fun showInTable(items: List<Almacen>) {
        // Agrupamos y además guardamos la lista original de Almacen en cada grupo
        val gruposConListas: List<Pair<GroupItem, List<Almacen>>> = items
            .groupBy { Triple(it.producto, it.marca, it.tamano) }
            .map { (key, listaCompleta) ->
                val (prod, brand, size) = key
                val fechas = listaCompleta.mapNotNull { it.fechaCaducidad }
                    .map { displaySdf.parse(it)!! }
                    .sorted()
                val grupo = GroupItem(
                    id = listaCompleta.first().id,
                    product = prod,
                    brand = brand,
                    size = size,
                    count = listaCompleta.size,
                    nextDate = fechas.firstOrNull()?.let { displaySdf.format(it) }
                )
                Pair(grupo, listaCompleta)
            }

        val table = binding.tableLayoutAlmacen
        if (table.childCount > 1) {
            table.removeViews(1, table.childCount - 1)
        }

        gruposConListas.forEach { (g, listaOriginal) ->
            val row = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }

            // Rellenar las celdas del TableRow
            listOf<Any>(
                g.count.toString(),
                g.product,
                g.brand,
                g.size,
                g.nextDate ?: "—"
            ).forEach { txt ->
                row.addView(TextView(this).apply {
                    setPadding(8, 8, 8, 8)
                    text = txt.toString()
                })
            }

            // Asociar el click para mostrar el PopupMenu mediante bindRowClick
            val itemRepresentante = listaOriginal.first()
            AlmacenItemActions(this).bindRowClick(row, itemRepresentante) {
                loadAlmacen()
            }

            table.addView(row)
            table.addView(View(this).apply {
                val h = (resources.displayMetrics.density + .5f).toInt()
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, h
                )
                setBackgroundColor(Color.BLACK)
            })
        }
    }
}
