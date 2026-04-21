package com.example.exer_fleximaze

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastShakeTime: Long = 0

    private lateinit var gameUI: View
    private lateinit var transitionLayout: View
    private lateinit var videoLayout: View
    private lateinit var scoreLayout: View

    private lateinit var mazeView: MazeView
    private lateinit var tvPontuacao: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRestartMessage: TextView

    private lateinit var tvTransitionTitle: TextView
    private lateinit var tvTransitionScore: TextView
    private lateinit var btnTransitionYes: Button
    private lateinit var btnTransitionNo: Button
    private lateinit var tvPausaDesc: TextView

    private lateinit var videoView: VideoView
    private lateinit var tvStars: TextView
    private lateinit var tvHighScore: TextView
    private lateinit var tvFinalScore: TextView

    private lateinit var soundPool: SoundPool
    private var somImpactoId: Int = 0

    private var nivelAtual = 1
    private var tempoSegundos = 0
    private var pontuacaoTotal = 0
    private var pontuacaoNivel = 5000

    private var acaoAposVideo: (() -> Unit)? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            tempoSegundos++

            pontuacaoNivel -= 5
            if (pontuacaoNivel < 500) pontuacaoNivel = 500

            val minutos = tempoSegundos / 60
            val segundos = tempoSegundos % 60

            tvTimer.text = String.format("Nível %d | ⏱ %02d:%02d", nivelAtual, minutos, segundos)
            tvPontuacao.text = "🏆 Pontos: ${pontuacaoTotal + pontuacaoNivel}"

            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameUI = findViewById(R.id.gameUI)
        transitionLayout = findViewById(R.id.transitionLayout)
        videoLayout = findViewById(R.id.videoLayout)
        scoreLayout = findViewById(R.id.scoreLayout)

        mazeView = findViewById(R.id.mazeView)
        tvPontuacao = findViewById(R.id.tvPontuacao)
        tvTimer = findViewById(R.id.tvTimer)
        tvRestartMessage = findViewById(R.id.tvRestartMessage)

        tvTransitionTitle = findViewById(R.id.tvTransitionTitle)
        tvTransitionScore = findViewById(R.id.tvTransitionScore)
        btnTransitionYes = findViewById(R.id.btnTransitionYes)
        btnTransitionNo = findViewById(R.id.btnTransitionNo)
        tvPausaDesc = findViewById(R.id.tvPausaDesc)

        videoView = findViewById(R.id.videoView)
        tvStars = findViewById(R.id.tvStars)
        tvHighScore = findViewById(R.id.tvHighScore)
        tvFinalScore = findViewById(R.id.tvFinalScore)

        val btnSkipVideo = findViewById<Button>(R.id.btnSkipVideo)
        val btnJogarNovamente = findViewById<Button>(R.id.btnJogarNovamente)
        val btnMenuPrincipal = findViewById<Button>(R.id.btnMenuPrincipal)

        // Configurar SoundPool para efeitos sonoros rápidos (como o impacto)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Carregar o som de impacto
        somImpactoId = soundPool.load(this, R.raw.somimpactobola, 1)

        carregarNivelComCores(nivelAtual)

        mazeView.onImpactListener = {
            // Tocar o som de impacto quando a bola bate na parede
            tocarSomImpacto()

            pontuacaoNivel -= 150
            if (pontuacaoNivel < 500) pontuacaoNivel = 500
            tvPontuacao.text = "🏆 Pontos: ${pontuacaoTotal + pontuacaoNivel}"
        }

        mazeView.onGoalReachedListener = {
            pararCronometro()
            sensorManager.unregisterListener(this)

            pontuacaoTotal += pontuacaoNivel

            if (nivelAtual < 3) {
                tvTransitionTitle.text = "Nível $nivelAtual Concluído! 👏"
                tvTransitionScore.text = "Pontuação do nível: $pontuacaoNivel\nTotal Acumulado: $pontuacaoTotal"
                tvPausaDesc.text = "Queres ver um vídeo rápido para relaxar as mãos e preparares-te para o próximo desafio?"
                btnTransitionNo.text = "Não (Avançar Rápido)"
            } else {
                tvTransitionTitle.text = "Labirintos Concluídos! 🎉"
                tvTransitionScore.text = "Pontuação do nível: $pontuacaoNivel\nPontuação Final! $pontuacaoTotal"
                tvPausaDesc.text = "Excelente trabalho! Queres fazer um alongamento relaxante antes de ver a classificação final?"
                btnTransitionNo.text = "Não (Ver Classificação)"
            }

            gameUI.visibility = View.GONE
            transitionLayout.visibility = View.VISIBLE
        }

        btnTransitionYes.setOnClickListener {
            transitionLayout.visibility = View.GONE
            if (nivelAtual < 3) {
                mostrarVideoAlongamento { avancarParaProximoNivel() }
            } else {
                mostrarVideoAlongamento { mostrarEcraClassificacao() }
            }
        }

        btnTransitionNo.setOnClickListener {
            transitionLayout.visibility = View.GONE
            if (nivelAtual < 3) {
                avancarParaProximoNivel()
            } else {
                mostrarEcraClassificacao()
            }
        }

        btnSkipVideo.setOnClickListener { fecharVideoEAplicarAcao() }
        btnJogarNovamente.setOnClickListener { reiniciarSessaoCompleta() }
        btnMenuPrincipal.setOnClickListener { finish() }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        iniciarCronometro()
    }

    private fun tocarSomImpacto() {
        // Toca o som com volume moderado (0.5f)
        soundPool.play(somImpactoId, 0.5f, 0.5f, 1, 0, 1f)
    }

    private fun avancarParaProximoNivel() {
        nivelAtual++

        pontuacaoNivel = 5000
        tempoSegundos = 0

        carregarNivelComCores(nivelAtual)

        gameUI.visibility = View.VISIBLE
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        iniciarCronometro()
    }

    private fun mostrarVideoAlongamento(acaoFinal: () -> Unit) {
        acaoAposVideo = acaoFinal

        videoLayout.visibility = View.VISIBLE

        val videoResId = when (nivelAtual) {
            1 -> R.raw.exercicio1
            2 -> R.raw.exercicio2
            3 -> R.raw.exercicio3
            else -> R.raw.exercicio1
        }

        val videoPath = "android.resource://$packageName/$videoResId"
        videoView.setVideoURI(Uri.parse(videoPath))

        videoView.setOnCompletionListener { fecharVideoEAplicarAcao() }
        videoView.start()
    }

    private fun fecharVideoEAplicarAcao() {
        videoView.stopPlayback()
        videoLayout.visibility = View.GONE
        acaoAposVideo?.invoke()
    }

    private fun mostrarEcraClassificacao() {
        scoreLayout.visibility = View.VISIBLE

        val estrelas = when {
            pontuacaoTotal >= 12000 -> "⭐⭐⭐"
            pontuacaoTotal >= 7000  -> "⭐⭐"
            else                    -> "⭐"
        }
        tvStars.text = estrelas

        val prefs = getSharedPreferences("FlexiMazePrefs", Context.MODE_PRIVATE)
        val recordeAntigo = prefs.getInt("RecordePontuacao", 0)

        tvFinalScore.text = pontuacaoTotal.toString()

        if (pontuacaoTotal > recordeAntigo) {
            prefs.edit().putInt("RecordePontuacao", pontuacaoTotal).apply()
            tvHighScore.text = "👑 $pontuacaoTotal 👑\n(Novo Recorde!)"
            tvHighScore.setTextColor(Color.parseColor("#FFEB3B"))
        } else {
            tvHighScore.text = recordeAntigo.toString()
            tvHighScore.setTextColor(Color.parseColor("#EAEAEA"))
        }
    }

    private fun reiniciarSessaoCompleta() {
        scoreLayout.visibility = View.GONE
        gameUI.visibility = View.VISIBLE

        nivelAtual = 1
        pontuacaoTotal = 0
        pontuacaoNivel = 5000
        tempoSegundos = 0

        carregarNivelComCores(nivelAtual)
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        iniciarCronometro()
    }

    private fun carregarNivelComCores(level: Int) {
        mazeView.loadLevel(level)

        val corSegura = when (level) {
            1 -> Color.parseColor("#4CAF50")
            2 -> Color.parseColor("#FF9800")
            3 -> Color.parseColor("#E53935")
            else -> Color.parseColor("#EAEAEA")
        }

        tvTimer.setTextColor(corSegura)
        tvPontuacao.setTextColor(corSegura)

        val timerBg = tvTimer.background as GradientDrawable
        timerBg.setStroke(2 * resources.displayMetrics.density.toInt(), corSegura)

        val pontuacaoBg = tvPontuacao.background as GradientDrawable
        pontuacaoBg.setStroke(2 * resources.displayMetrics.density.toInt(), corSegura)
    }

    private fun iniciarCronometro() {
        pararCronometro()
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun pararCronometro() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun reiniciarNivelAtual() {
        mazeView.resetPosition()
        tempoSegundos = 0
        pontuacaoNivel = 5000

        tvTimer.text = String.format("Nível %d | ⏱ 00:00", nivelAtual)
        tvPontuacao.text = "🏆 Pontos: ${pontuacaoTotal + pontuacaoNivel}"

        tvRestartMessage.visibility = View.VISIBLE
        tvRestartMessage.alpha = 1f
        tvRestartMessage.animate()
            .alpha(0f)
            .setDuration(2500)
            .withEndAction { tvRestartMessage.visibility = View.GONE }
            .start()
    }

    override fun onResume() {
        super.onResume()
        if (gameUI.visibility == View.VISIBLE) {
            rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            iniciarCronometro()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        pararCronometro()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        pararCronometro()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (gameUI.visibility != View.VISIBLE || event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                mazeView.moveBall(orientationAngles[1], orientationAngles[2])
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble())

                if (gForce > 1.8) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 1500) {
                        lastShakeTime = now
                        reiniciarNivelAtual()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}