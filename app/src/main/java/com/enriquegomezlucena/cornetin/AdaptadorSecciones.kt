package com.enriquegomezlucena.cornetin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador que muestra encabezados de sección (categorías) y toques individuales
class AdaptadorSecciones(
    private val elementos: List<Any>,
    private val onEditarClick: (Toques) -> Unit,
    private val onEliminarClick: (Toques) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TIPO_ENCABEZADO = 0   // Encabezado de sección (categoría)
        const val TIPO_TOQUE = 1       // Toque individual
    }

    // ViewHolder para encabezado de sección
    class VistaEncabezado(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textoEncabezado: TextView = itemView.findViewById(R.id.txtEncabezado)
    }

    // ViewHolder para un toque individual
    class VistaToque(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textoNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val textoHora: TextView = itemView.findViewById(R.id.txtHora)
        val textoRepeticion: TextView = itemView.findViewById(R.id.txtRepeticion)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
        // Aquí podrías añadir más TextViews si quieres mostrar la categoría o el mes del toque
    }

    // Determina el tipo de vista (encabezado o toque)
    override fun getItemViewType(position: Int): Int {
        return if (elementos[position] is String) TIPO_ENCABEZADO else TIPO_TOQUE
    }

    // Crea el ViewHolder adecuado según el tipo de elemento
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENCABEZADO) {
            val vista =
                LayoutInflater.from(parent.context).inflate(R.layout.item_encabezado, parent, false)
            VistaEncabezado(vista)
        } else {
            val vista =
                LayoutInflater.from(parent.context).inflate(R.layout.item_toque, parent, false)
            VistaToque(vista)
        }
    }

    // Asocia los datos a cada vista
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VistaEncabezado) {
            holder.textoEncabezado.text = elementos[position] as String
        } else if (holder is VistaToque) {
            val toque = elementos[position] as Toques

            holder.textoNombre.text = toque.nombre
            holder.textoHora.text = toque.hora
            holder.textoRepeticion.text = toque.repeticion

            // Acción al hacer clic en el toque: abrir pantalla de detalle
            holder.itemView.setOnClickListener {
                val contexto = it.context
                val intent = Intent(contexto, DetalleToqueActivity::class.java).apply {
                    putExtra("nombre", toque.nombre)
                    putExtra("hora", toque.hora)
                    putExtra("repeticion", toque.repeticion)
                    putExtra("sonido", toque.sonido)
                    // Aquí podrías añadir también: putExtra("categoria", toque.categoria)
                }
                contexto.startActivity(intent)
            }

            // Botón de editar
            holder.btnEditar.setOnClickListener {
                onEditarClick(toque)
            }

            // Botón de eliminar
            holder.btnEliminar.setOnClickListener {
                onEliminarClick(toque)
            }
        }
    }

    // Número total de elementos en la lista
    override fun getItemCount(): Int = elementos.size
}
