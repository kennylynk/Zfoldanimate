package com.example.foldanim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Góc gập mặc định ban đầu là 90 độ để thấy ngay hiệu ứng 3D
    var userControlledAngle by remember { mutableFloatStateOf(90f) }

    // Hỗ trợ vuốt tay trực tiếp trên màn hình để kéo mở / gập 3D
    val interactiveModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                userControlledAngle = (userControlledAngle + (dragAmount.x / 3f)).coerceIn(0f, 180f)
            }
        )
    }

    // Nội suy mượt mà chuyển động
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
            // NỬA TRÁI 3D (Màu xanh dương)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = leftRotation
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        cameraDistance = 8f * density
                    }
                    .background(Color(0xFF1E88E5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TRÁI",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // NỬA PHẢI 3D (Màu xanh lá)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = rightRotation
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = 8f * density
                    }
                    .background(Color(0xFF43A047)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PHẢI",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // BÓNG ĐỔ NẾP GẤP Ở GIỮA
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .graphicsLayer { alpha = shadowAlpha }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black, Color.Transparent)
                    )
                )
        )
    }
}
