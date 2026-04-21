package com.example.exer_fleximaze

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar e configurar a música de fundo
        iniciarMusica()

        // Ligar o código aos botões do XML
        val btnJogar = findViewById<Button>(R.id.btnJogar)
        val btnInstrucoes = findViewById<Button>(R.id.btnInstrucoes)

        // Ação do botão "Jogar"
        btnJogar.setOnClickListener {
            // Tocar som ao clicar no botão
            tocarSomClique()

            // Cria uma "Intenção" de navegar da MainActivity para a GameActivity
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão "Instruções"
        btnInstrucoes.setOnClickListener {
            mostrarCaixaInstrucoes()
        }
    }

    private fun iniciarMusica() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.somintro)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.5f, 0.5f) // Volume reduzido para não ser incomodativo
        }
        mediaPlayer?.start()
    }

    private fun tocarSomClique() {
        val mp = MediaPlayer.create(this, R.raw.somjogar)
        mp.setOnCompletionListener { 
            it.release() 
        }
        mp.start()
    }

    override fun onResume() {
        super.onResume()
        // Retomar a música se o utilizador voltar ao ecrã inicial
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        // Pausar a música quando sai do ecrã inicial (ex: vai para o jogo)
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Libertar recursos ao fechar a aplicação
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
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