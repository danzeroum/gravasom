package com.gravasom.core.service.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.gravasom.core.service.RecordingService

/** Quick Settings Tile: um toque inicia ou para a gravacao. */
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        startForegroundService(intent)
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }
}
