package com.grupoa.ucompensamarket

class Productos {
    var uid: String? = null
    var nombre: String? = null
    var descripcion: String? = null
    var precio: Double? = null
    var imagenUrl: String? = ""

    constructor()

    constructor(uid: String?, nombre: String?, descripcion: String?, precio: Double?, imagenUrl: String? ) {
        this.uid = uid
        this.nombre = nombre
        this.descripcion = descripcion
        this.precio = precio
        this.imagenUrl = imagenUrl
    }
}


data class Producto(
    var uid: String? = null,
    var nombre: String? = null,
    var descripcion: String? = null,
    var precio: Double = 0.0,
    var imagenUrl: String? = ""
)