package com.grupoa.ucompensamarket

// Modelo para items del carrito. Compatible con Firebase Realtime Database:
// - constructor vacío
// - propiedades mutables (var)
class CartItem() {
    var productId: String? = null
    var nombre: String? = null
    var precio: Double = 0.0
    var imagenUrl: String? = ""
    var cantidad: Int = 1

    constructor(
        productId: String?,
        nombre: String?,
        precio: Double,
        imagenUrl: String?,
        cantidad: Int
    ) : this() {
        this.productId = productId
        this.nombre = nombre
        this.precio = precio
        this.imagenUrl = imagenUrl
        this.cantidad = cantidad
    }
}