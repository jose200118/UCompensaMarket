package com.grupoa.ucompensamarket

import kotlin.Double

class Productos {
    var uid: String? = null
    var nombre: String? = null
    var descripcion: String? = null
    var precio: Double? = null
    var imagenUrl: String? = ""
    var lat: Double? = null
    var lng: Double? = null

    constructor()

    constructor(uid: String?, nombre: String?, descripcion: String?, precio: Double?, imagenUrl: String?, lat: Double?, lng: Double? ) {
        this.uid = uid
        this.nombre = nombre
        this.descripcion = descripcion
        this.precio = precio
        this.imagenUrl = imagenUrl
        this.lat = lat
        this.lng = lng
    }
}


data class Producto(
    var uid: String? = null,
    var nombre: String? = null,
    var descripcion: String? = null,
    var precio: Double = 0.0,
    var imagenUrl: String? = "",
    var lat: Double? = null,
    var lng: Double? = null
)