package com.example.foldanim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color // <-- Thêm import này để sửa lỗi Unresolved reference
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuoFoldScreen()
        }
    }
}

@Composable
fun DuoFoldScreen() {
    var userControlledAngle by remember { mutableFloatStateOf(90f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    val interactiveModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { isUserInteracting = true },
            onDragEnd = { isUserInteracting = false },
            onDragCancel = { isUserInteracting = false },
            onDrag = { change, dragAmount ->
                change.consume()
                userControlledAngle = (userControlledAngle + (dragAmount.x / 3f)).coerceIn(0f, 180f)
            }
        )
    }

    val smoothAngle by animateFloatAsState(
        targetValue = userControlledAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SmoothHingeAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(interactiveModifier),
        contentAlignment = Alignment.Center
    ) {
        val fraction = smoothAngle / 180f
        val leftRotation = (180f - smoothAngle) / 2f
        val rightRotation = -(180f - smoothAngle) / 2f
        val globalScale = 0.80f + (0.20f * fraction)
        val shadowAlpha = (1f - fraction).toDouble().pow(2).toFloat()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = globalScale
                    scaleY = globalScale
                }
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = leftRotation
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        cameraDistance = 16f * density
                    }
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.sym_def_app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize(2f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = rightRotation
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = 16f * density
                    }
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.sym_def_app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxSize(2f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .graphicsLayer { alpha = shadowAlpha }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black, Color.Transparent)
                    )
                )
        )
    }
}
