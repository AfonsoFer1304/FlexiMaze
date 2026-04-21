package com.example.exer_fleximaze

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class MazeView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // --- Pincéis de Desenho ---
    private val gridPaint = Paint().apply {
        color = Color.argb(15, 255, 255, 255)
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val wallFillPaint = Paint().apply { style = Paint.Style.FILL }
    private val wallStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val goalPaint = Paint().apply { style = Paint.Style.FILL }
    private val ballPaint = Paint().apply {
        isAntiAlias = true
        setShadowLayer(8f, 4f, 4f, Color.argb(120, 0, 0, 0))
    }

    // --- Mapas dos Níveis ---
    private val mapLevel1 = arrayOf(
        "11111111111", "1S000100001", "11110101101", "10000001001",
        "10111111111", "10000000001", "11111111101", "10000000101",
        "10111110101", "10100000101", "10101111101", "10001000001",
        "11111011111", "10000000021", "11111111111"
    )

    private val mapLevel2 = arrayOf(
        "111111111111111", "1S0001000000001", "101101011111101", "100001000000101",
        "101111111110101", "101000000000101", "101011111110101", "101010000000101",
        "101010111110101", "101010120010101", "101010111010101", "101010001010101",
        "101011111010101", "101000000010101", "101111111110101", "101000000000101",
        "101111111111101", "100000000000001", "111111111111111"
    )

    private val mapLevel3 = arrayOf(
        "11111111111111111", "1S000000100000001", "10111110101111101", "10100010101000101",
        "10101010101010101", "11100000101010101", "10001111101010101", "10100000001010101",
        "10111111111010101", "10000000000000101", "11111111111110101", "10000000000010101",
        "10111111111010101", "10100000001010101", "10101111101010101", "10101000000000101",
        "10101011111111111", "10101000000000001", "10101111101111101", "10000000000000001",
        "11111111111111101", "10000000000000201", "11111111111111111"
    )

    private var currentMap = mapLevel1
    private var bgColor = Color.parseColor("#0A1128")

    // --- Variáveis do Sistema ---
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private val walls = mutableListOf<RectF>()
    private var goalRect = RectF()

    private var ballX = 0f;
    private var ballY = 0f
    private var startX = 0f;
    private var startY = 0f
    private var ballRadius = 0f

    var onImpactListener: (() -> Unit)? = null
    var onGoalReachedListener: (() -> Unit)? = null

    private var isGameOver = false
    private var isTouchingWall = false

    fun loadLevel(level: Int) {
        when (level) {
            1 -> {
                currentMap = mapLevel1
                wallFillPaint.color = Color.parseColor("#4CAF50")
                wallStrokePaint.color = Color.parseColor("#FFFFFF")
                goalPaint.color = Color.parseColor("#00E676")
                goalPaint.setShadowLayer(20f, 0f, 0f, Color.parseColor("#69F0AE"))
            }

            2 -> {
                currentMap = mapLevel2
                wallFillPaint.color = Color.parseColor("#FF9800")
                wallStrokePaint.color = Color.parseColor("#FFFFFF")
                goalPaint.color = Color.parseColor("#FFAB00")
                goalPaint.setShadowLayer(20f, 0f, 0f, Color.parseColor("#FFD740"))
            }

            3 -> {
                currentMap = mapLevel3
                wallFillPaint.color = Color.parseColor("#E53935")
                wallStrokePaint.color = Color.parseColor("#FFFFFF")
                goalPaint.color = Color.parseColor("#FF1744")
                goalPaint.setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF8A80"))
            }
        }

        wallFillPaint.setShadowLayer(10f, 6f, 6f, Color.argb(80, 0, 0, 0))

        if (screenWidth > 0) {
            construirLabirinto()
            resetPosition()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat(); screenHeight = h.toFloat()
        construirLabirinto()
        resetPosition()
    }

    private fun construirLabirinto() {
        walls.clear()
        val numRows = currentMap.size;
        val numCols = currentMap[0].length

        cellSize = Math.min(screenWidth / numCols, screenHeight / numRows)
        offsetX = (screenWidth - (numCols * cellSize)) / 2f
        offsetY = (screenHeight - (numRows * cellSize)) / 2f

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val char = currentMap[row][col]
                val left = offsetX + col * cellSize;
                val top = offsetY + row * cellSize
                val right = left + cellSize;
                val bottom = top + cellSize

                when (char) {
                    '1' -> walls.add(RectF(left, top, right, bottom))
                    '2' -> goalRect = RectF(left, top, right, bottom)
                    'S' -> {
                        startX = left + cellSize / 2f; startY = top + cellSize / 2f
                    }
                }
            }
        }
        ballRadius = cellSize * 0.30f
    }

    fun resetPosition() {
        ballX = startX; ballY = startY; isGameOver = false; isTouchingWall = false; invalidate()
    }

    fun moveBall(pitch: Float, roll: Float) {
        if (isGameOver) return
        val speed = 35f;
        val dx = roll * speed;
        val dy = -pitch * speed
        val nextX = ballX + dx;
        val nextY = ballY + dy
        val ballRect =
            RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius)

        if (RectF.intersects(ballRect, goalRect)) {
            isGameOver = true; onGoalReachedListener?.invoke(); invalidate(); return
        }

        var currentlyTouching = false
        val testXRect =
            RectF(nextX - ballRadius, ballY - ballRadius, nextX + ballRadius, ballY + ballRadius)
        var hitX = false
        for (wall in walls) {
            if (RectF.intersects(testXRect, wall)) hitX = true
        }
        if (!hitX) ballX = nextX else currentlyTouching = true

        val testYRect =
            RectF(ballX - ballRadius, nextY - ballRadius, ballX + ballRadius, nextY + ballRadius)
        var hitY = false
        for (wall in walls) {
            if (RectF.intersects(testYRect, wall)) hitY = true
        }
        if (!hitY) ballY = nextY else currentlyTouching = true

        if (currentlyTouching && !isTouchingWall) {
            onImpactListener?.invoke()
        }
        isTouchingWall = currentlyTouching; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // RESTAURADO: Pinta o fundo sólido para o jogo não mostrar a imagem do menu
        canvas.drawColor(bgColor)

        val espacamento = 60f;
        var xGrid = 0f
        while (xGrid < screenWidth) {
            canvas.drawLine(xGrid, 0f, xGrid, screenHeight, gridPaint); xGrid += espacamento
        }
        var yGrid = 0f
        while (yGrid < screenHeight) {
            canvas.drawLine(0f, yGrid, screenWidth, yGrid, gridPaint); yGrid += espacamento
        }

        canvas.drawRect(goalRect, goalPaint)

        for (wall in walls) {
            canvas.drawRect(wall, wallFillPaint); canvas.drawRect(wall, wallStrokePaint)
        }

        ballPaint.shader = RadialGradient(
            ballX - ballRadius / 3f, ballY - ballRadius / 3f, ballRadius * 1.2f,
            intArrayOf(
                Color.parseColor("#80B3FF"),
                Color.parseColor("#1966FF"),
                Color.parseColor("#002699")
            ), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
    }
}
