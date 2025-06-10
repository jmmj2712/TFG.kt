package com.example.proyectotfg.Api

/**
 * Petición para agregar nuevas fechas de caducidad al almacén de un producto existente.
 *
 * @property producto_id  Identificador único del producto al que se añadirán las fechas.
 * @property cantidad     Número de unidades que se desean registrar en el almacén.
 * @property fechaCaducidad  Lista de fechas de caducidad en formato CSV ("dd/MM/yyyy,...").
 */
data class AddAlmacenRequest(
    val producto_id: Int,
    val cantidad: Int,
    val fechaCaducidad: String
)

