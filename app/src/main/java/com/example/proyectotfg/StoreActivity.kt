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

/**
 * Actividad para gestionar y mostrar el inventario de almacén.
 *
 * - Descarga todos los registros (`almacen JOIN productos`) y programa notificaciones
 *   de caducidad.
// - Permite filtrar por nombre, marca, tamaño o fecha de caducidad.
// - Agrupa los items iguales (mismo producto, marca y tamaño) mostrando recuento y próxima fecha.
 */
class StoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreBinding

    // Lista completa de registros recibidos de la API
    private var allItems = listOf<Almacen>()

    // Formato para mostrar/parsear fechas en pantalla
    private val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Datos agrupados para mostrar en la tabla:
     * @param id          ID representativo del grupo (se toma el primero)
     * @param product     Nombre del producto
     * @param brand       Marca
     * @param size        Tamaño ("Pequeño"/"Grande")
     * @param count       Número de unidades en este grupo
     * @param nextDate    Fecha mínima de caducidad formateada, o null si no aplica
     */
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

        // Inicializamos textos de botones de filtro
        binding.btnBrandFilter.text = "Todas"
        binding.btnSizeFilter.text = "Tamaño"
        binding.btnDateFilter.text = "Fecha"

        // Configuración de listeners para botones
        binding.buttonVolver.setOnClickListener { finish() }
        binding.btnResetFilters.setOnClickListener { resetFilters() }
        binding.btnApplyNameFilter.setOnClickListener { applyFilters() }
        binding.btnBrandFilter.setOnClickListener { showBrandMenu() }
        binding.btnSizeFilter.setOnClickListener { showSizeMenu() }
        binding.btnDateFilter.setOnClickListener { pickCutoffDate() }

        // Carga inicial de datos
        loadAlmacen()
    }

    /**
     * Lanza la petición a la API para descargar todos los registros de almacén.
     * Programa recordatorios de caducidad para cada item recibido.
     */
    private fun loadAlmacen() {
        RetrofitClient.instance.getAlmacen().enqueue(object : Callback<List<Almacen>> {
            override fun onResponse(call: Call<List<Almacen>>, resp: Response<List<Almacen>>) {
                if (!resp.isSuccessful) {
                    Toast.makeText(this@StoreActivity, "Error ${resp.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                allItems = resp.body().orEmpty()
                // Programar notificaciones 2 semanas antes de cada caducidad
                allItems.forEach { NotificationWorker.programar(this@StoreActivity, it) }
                showInTable(allItems)
            }

            override fun onFailure(call: Call<List<Almacen>>, t: Throwable) {
                Toast.makeText(this@StoreActivity, "Fallo API: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Restablece todos los filtros y muestra la tabla completa. */
    private fun resetFilters() {
        binding.etFilterName.text?.clear()
        binding.btnBrandFilter.text = "Todas"
        binding.btnSizeFilter.text = "Tamaño"
        binding.btnDateFilter.text = "Fecha"
        showInTable(allItems)
    }

    /** Muestra un menú emergente con las marcas disponibles para filtrar. */
    private fun showBrandMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnBrandFilter)
        popup.menu.add("Todas")
        // Añade cada marca única de allItems al menú
        allItems.mapNotNull { it.marca }.distinct().sorted()
            .forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { mi ->
            binding.btnBrandFilter.text = mi.title
            applyFilters()
            true
        }
        popup.show()
    }

    /** Muestra un menú emergente para seleccionar filtro de tamaño (Grande/Pequeño). */
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

    /**
     * Abre un selector de fecha para elegir la fecha de corte.
     * Formatea el botón y vuelve a aplicar filtros.
     */
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

    /**
     * Aplica todos los filtros seleccionados:
     *  - Nombre (contiene texto)
     *  - Marca (si no es "Todas")
     *  - Tamaño (si no es "Tamaño")
     *  - Fecha de caducidad (sólo items con fecha ≤ seleccionada)
     * Muestra un diálogo si no hay resultados.
     */
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

    /**
     * Agrupa los [items] por (producto, marca, tamaño), calcula el número de unidades
     * y la próxima fecha de caducidad, y construye dinámicamente las filas de la tabla.
     * Cada fila se asocia a acciones (Modificar, Eliminar, Consumir) mediante AlmacenItemActions.
     */
    private fun showInTable(items: List<Almacen>) {
        // Agrupación clave → lista de instancias
        val gruposConListas: List<Pair<GroupItem, List<Almacen>>> = items
            .groupBy { Triple(it.producto, it.marca, it.tamano) }
            .map { (key, listaCompleta) ->
                val (prod, brand, size) = key
                // Extrae y ordena fechas de caducidad para cada grupo
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
        // Limpia filas previas (dejando sólo encabezado)
        if (table.childCount > 1) {
            table.removeViews(1, table.childCount - 1)
        }

        // Construcción de cada fila y separador
        gruposConListas.forEach { (g, listaOriginal) ->
            val row = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }

            // Añade celdas: count, producto, marca, tamaño, nextDate
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

            // Asocia menú de acciones para este grupo
            val itemRepresentante = listaOriginal.first()
            AlmacenItemActions(this).bindRowClick(row, itemRepresentante) {
                loadAlmacen()  // recarga tras operación
            }

            table.addView(row)
            // Línea divisoria
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
