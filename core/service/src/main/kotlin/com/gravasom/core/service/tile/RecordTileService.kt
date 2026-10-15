package com.gravasom.core.service.tile

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState

/** Quick Settings Tile: um toque inicia a gravacao; com gravacao em curso, o toque a para. */
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        when {
            emCurso() -> startService(Intent(this, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> abrirApp()
            else -> startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.ACTION_START))
        }
        refresh()
    }

    private fun emCurso(): Boolean = RecordingServiceState.state.value.let { it is RecordingState.Recording || it == RecordingState.Paused }

    private fun refresh() {
        qsTile?.state = if (emCurso()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    /**
     * Sem permissao de microfone nao ha como iniciar o servico em primeiro plano (o sistema derrubaria o app);
     * abre o app, onde o usuario concede.
     */
    @Suppress("DEPRECATION")
    private fun abrirApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(launch)
        }
    }
}
