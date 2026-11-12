package dev.rakara.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rakara.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin


private const val TAGLINE = "Setiap halaman, cerita baru."

@Composable
fun SplashScreen() {
    val bg = MaterialTheme.colorScheme.background
    val navy = MaterialTheme.colorScheme.primary
    val iris = MaterialTheme.colorScheme.secondary
    val gold = MaterialTheme.colorScheme.tertiary

    
    val grad = rememberInfiniteTransition(label = "bg")
    val phase by grad.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    
    val c1 = lerp(navy.copy(alpha = 0.18f), iris.copy(alpha = 0.14f), phase)
    val c2 = lerp(iris.copy(alpha = 0.12f), gold.copy(alpha = 0.10f), phase)
    val c3 = lerp(gold.copy(alpha = 0.08f), navy.copy(alpha = 0.16f), phase)

    
    val startX = -200f + phase * 400f
    val startY = -100f + phase * 200f
    val endX = 1400f + phase * 300f
    val endY = 2200f + phase * 400f

    
    val mascotP = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        mascotP.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 350f))
    }

    
    val idle = rememberInfiniteTransition(label = "float")
    val floatPhase by idle.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "float"
    )
    val mascotFloat = sin(floatPhase) * 6f  

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .background(
                Brush.linearGradient(
                    colors = listOf(bg, c1, c2, c3, bg),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        
        Box(
            Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            iris.copy(alpha = 0.22f + phase * 0.08f),
                            iris.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = mascotP.value
                    scaleY = mascotP.value
                    alpha = mascotP.value.coerceIn(0f, 1f)
                }
                .offset(y = mascotFloat.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.mascot_splash),
                contentDescription = "Mascot",
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "RAKARA",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                TAGLINE,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        
        Text(
            "Powered by HynchLabs",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}