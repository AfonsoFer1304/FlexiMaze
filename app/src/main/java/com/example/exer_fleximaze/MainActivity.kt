package com.example.exer_fleximaze

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ligar o código aos botões do XML
        val btnJogar = findViewById<Button>(R.id.btnJogar)
        val btnInstrucoes = findViewById<Button>(R.id.btnInstrucoes)

        // Ação do botão "Jogar"
        btnJogar.setOnClickListener {
            // Cria uma "Intenção" de navegar da MainActivity para a GameActivity
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão "Instruções"
        btnInstrucoes.setOnClickListener {
            mostrarCaixaInstrucoes()
        }
    }

    private fun mostrarCaixaInstrucoes() {
        // Constrói e mostra uma caixa de diálogo elegante
        AlertDialog.Builder(this)
            .setTitle("Como Jogar")
            .setMessage("🎯 Objetivo:\nConduz a esfera até à meta para exercitares os teus pulsos.\n\n" +
                    "📱 Controlo:\nInclina o telemóvel suavemente para moveres a esfera.\n\n" +
                    "⚡ Reset Rápido:\nSe ficares preso, dá um abanão rápido (shake) ao telemóvel para reiniciar a posição.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss() // Fecha a caixa quando clicado
            }
            .setCancelable(false) // Obriga o utilizador a clicar no botão "Entendido"
            .show()
    }
}