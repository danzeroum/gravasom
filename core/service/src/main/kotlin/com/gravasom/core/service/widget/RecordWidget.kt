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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.content.Intent
import com.gravasom.core.service.R
import com.gravasom.core.service.RecordingService

/** Widget Glance com dois alvos: Gravar (inicia a gravacao) e Marcar (marca o instante da gravacao em curso). */
class RecordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val startAction = actionStartService(serviceIntent(context, RecordingService.ACTION_START), isForegroundService = true)
        // O servico de gravacao ja esta em primeiro plano quando ha o que marcar; sem gravacao ele ignora a acao e encerra.
        val markerAction = actionStartService(serviceIntent(context, RecordingService.ACTION_MARKER), isForegroundService = false)
        Row(modifier = GlanceModifier.fillMaxSize().background(Color(0xFF12151A))) {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(startAction), contentAlignment = Alignment.Center) {
                Text(text = context.getString(R.string.widget_record), style = TextStyle(color = ColorProvider(Color.White)))
            }
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(markerAction), contentAlignment = Alignment.Center) {
                Text(text = context.getString(R.string.widget_marker), style = TextStyle(color = ColorProvider(Color(MARKER_COLOR))))
            }
        }
    }

    private fun serviceIntent(context: Context, action: String): Intent =
        Intent(context, RecordingService::class.java).setAction(action)

    private companion object {
        const val MARKER_COLOR = 0xFFFFC107
    }
}
