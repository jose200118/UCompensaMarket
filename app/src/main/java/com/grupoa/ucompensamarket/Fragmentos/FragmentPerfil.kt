package com.grupoa.ucompensamarket.Fragmentos

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.grupoa.ucompensamarket.Constantes
import com.grupoa.ucompensamarket.MensajeBienvenidaActivity
import com.grupoa.ucompensamarket.R
import com.grupoa.ucompensamarket.databinding.FragmentPerfilBinding

class FragmentPerfil : Fragment() {
    private lateinit var binding : FragmentPerfilBinding
    private lateinit var mContext : Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentPerfilBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // Inicializar progressDialog correctamente
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        cargarInformacion()

        binding.btnGuardar.setOnClickListener {
            validarInformacion()
        }

        binding.btnCerrarLogin.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, MensajeBienvenidaActivity::class.java))
            activity?.finishAffinity()
        }
    }

    private fun cargarInformacion() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            // No hay usuario logueado
            Toast.makeText(
                mContext,
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val Uid = snapshot.child("uid").value
                    val Unombres = "${snapshot.child("nombres").value}"
                    val Uemail = "${snapshot.child("email").value}"
                    val Utelefono = "${snapshot.child("telefono").value}"
                    val Uimagen = "${snapshot.child("imagen").value}"
                    val Uinfo = "${snapshot.child("info").value}"
                    val Uprovedor = "${snapshot.child("proveedor").value}"
                    var Utiempo_r = "${snapshot.child("tiempoR").value}"
                    val Upolitica = "${snapshot.child("politica").value}"

                    if(Utiempo_r == "null" || Utiempo_r.isBlank()) {
                        Utiempo_r = "0"
                    }

                    // Conversion de Fecha (si Utiempo_r puede ser no numérico, captura excepciones)
                    val fecha = try {
                        Constantes.formatoFecha(Utiempo_r.toLong())
                    } catch (e: Exception) {
                        ""
                    }

                    // Poner la informacion en la vista
                    binding.etEmail.setText(Uemail)
                    binding.etNombre.setText(Unombres)
                    binding.etInfo.setText(Uinfo)
                    binding.etTelefono.setText(Utelefono)

                    // Cargar imagen si existe y no es "null"
                    try {
                        if (!Uimagen.isNullOrBlank() && Uimagen != "null") {
                            Glide.with(mContext)
                                .load(Uimagen)
                                .placeholder(R.drawable.ic_default_user_avatar)
                                .into(binding.imgProfileAvatar)
                        }
                        else {
                            binding.imgProfileAvatar.setImageResource(R.drawable.ic_default_user_avatar)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            mContext,
                            "${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar el error de lectura
                    Toast.makeText(
                        mContext,
                        "Error al leer datos: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private var nombres = ""
    private var telefono = ""
    private var imagen = ""
    private var info = ""

    private fun validarInformacion() {
        //imagen = binding.imgProfileAvatar.text.toString().trim()
        nombres = binding.etNombre.text.toString().trim()
        info = binding.etInfo.text.toString().trim()
        telefono = binding.etTelefono.text.toString().trim()

        if(nombres.isEmpty()) {
            binding.etNombre.error = "El campo es requerido"
            binding.etNombre.requestFocus()
        }
        else if (telefono.isEmpty()){
            binding.etTelefono.error = "El campo es requerido"
            binding.etTelefono.requestFocus()
        }
        else {
            actualizarInformacion()
        }
    }

    private fun actualizarInformacion() {
        // Asegurarnos de que progressDialog está inicializado
        if (!::progressDialog.isInitialized) {
            progressDialog = ProgressDialog(requireContext())
            progressDialog.setTitle("Espere por favor")
            progressDialog.setCanceledOnTouchOutside(false)
        }

        progressDialog.setMessage("Actualizando informacion")
        progressDialog.show()

        val hashMap : HashMap<String, Any> = HashMap()
        hashMap["nombres"] = nombres
        hashMap["info"] = info
        hashMap["telefono"] = telefono

        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            progressDialog.dismiss()
            Toast.makeText(mContext, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(
                    mContext,
                    "Se actualizo correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    mContext,
                    "${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}