package com.gravasom.core.service.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Widget Glance com um unico alvo: iniciar a gravacao ao abrir o app. */
class RecordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(0xFF12151A.toInt()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Gravar",
                style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())),
            )
        }
    }
}
