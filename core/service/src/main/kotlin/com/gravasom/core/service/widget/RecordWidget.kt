package com.gravasom.core.service.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.content.Intent
import com.gravasom.core.service.RecordingService

/** Widget Glance com um unico alvo: iniciar a gravacao ao abrir o app. */
class RecordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val startAction = actionStartService(startIntent(LocalContext.current), isForegroundService = true)
        Box(
            modifier = GlanceModifier.fillMaxSize().background(Color(0xFF12151A)).clickable(startAction),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Gravar",
                style = TextStyle(color = ColorProvider(Color.White)),
            )
        }
    }

    private fun startIntent(context: Context): Intent =
        Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_START)
}
