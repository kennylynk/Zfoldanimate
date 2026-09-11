package com.example.foldanim

import android.animation.ValueAnimator
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.launch
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private var lastFoldState: FoldingFeature.State? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info -> handleLayoutInfo(info) }
            }
        }
    }

    private fun handleLayoutInfo(info: WindowLayoutInfo) {
        val folding = info.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull() ?: return

        if (folding.state != lastFoldState) {
            lastFoldState = folding.state
            playTransition()
        }
    }

    private fun playTransition() {
        val outer = findViewById<View>(R.id.layoutOuter)
        val inner = findViewById<View>(R.id.layoutInner)

        val showingInner = inner.alpha > 0.5f
        val fadeOutView = if (showingInner) inner else outer
        val fadeInView = if (showingInner) outer else inner
        fadeInView.bringToFront()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 420
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { anim ->
            val t = anim.animatedValue as Float
            fadeOutView.alpha = 1f - t
            fadeInView.alpha = t

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurRadius = (14f * sin(t * Math.PI)).toFloat().coerceAtLeast(0.01f)
                val effect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                fadeOutView.setRenderEffect(effect)
                fadeInView.setRenderEffect(effect)
            }
        }

        animator.doOnEnd {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                fadeOutView.setRenderEffect(null)
                fadeInView.setRenderEffect(null)
            }
        }

        animator.start()
    }
}
