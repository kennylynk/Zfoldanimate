package com.example.foldanim

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.View

/**
 * Draws two generated "screen" layers and dissolves between them using
 * an AGSL shader, driven by the live hinge-angle sensor value.
 */
class FoldShaderView(context: Context) : View(context) {

    private var progress = 0f
    private var runtimeShader: RuntimeShader? = null
    private var bitmapA: Bitmap? = null
    private var bitmapB: Bitmap? = null
    private val paint = Paint()

    private val agslSource = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float progress;
        uniform float2 resolution;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float edge = smoothstep(progress - 0.06, progress + 0.06, uv.y);
            half4 colorA = imageA.eval(fragCoord);
            half4 colorB = imageB.eval(fragCoord);
            return mix(colorB, colorA, edge);
        }
    """.trimIndent()

    fun setProgress(value: Float) {
        progress = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        bitmapA = generateLayer(w, h, intArrayOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt()))
        bitmapB = generateLayer(w, h, intArrayOf(0xFF1E3A8A.toInt(), 0xFF60A5FA.toInt()))

        val shader = RuntimeShader(agslSource)
        shader.setInputShader(
            "imageA",
            BitmapShader(bitmapA!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        )
        shader.setInputShader(
            "imageB",
            BitmapShader(bitmapB!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        )
        shader.setFloatUniform("resolution", w.toFloat(), h.toFloat())
        runtimeShader = shader
        paint.shader = shader
    }

    private fun generateLayer(w: Int, h: Int, colors: IntArray): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val gradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(), colors, null, Shader.TileMode.CLAMP
        )
        val p = Paint()
        p.shader = gradient
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        val shader = runtimeShader ?: return
        shader.setFloatUniform("progress", progress)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
