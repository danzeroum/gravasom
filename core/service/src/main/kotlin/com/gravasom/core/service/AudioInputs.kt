package com.gravasom.core.service

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/** Uma entrada de audio disponivel (microfone do celular, USB, Bluetooth...). */
data class InputOption(val id: Int, val label: String)

/** Lista as entradas de audio e descreve a ativa (etapa "Gravar" da jornada da Carla: gravar pelo microfone certo). */
object AudioInputs {

    /** AudioDeviceInfo.TYPE_BLE_HEADSET (valor 26) so existe como constante na API 31; o tipo em si e aceito antes. */
    private const val TYPE_BLE_HEADSET = 26

    private val usaveis = setOf(
        AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, TYPE_BLE_HEADSET,
    )

    private val rotulos = mapOf(
        AudioDeviceInfo.TYPE_BUILTIN_MIC to "Microfone do celular",
        AudioDeviceInfo.TYPE_WIRED_HEADSET to "Fone com fio",
        AudioDeviceInfo.TYPE_USB_DEVICE to "USB",
        AudioDeviceInfo.TYPE_USB_HEADSET to "USB",
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO to "Bluetooth",
        TYPE_BLE_HEADSET to "Bluetooth",
    )

    private fun manager(context: Context) = context.getSystemService(AudioManager::class.java)

    private fun devices(context: Context): List<AudioDeviceInfo> =
        manager(context).getDevices(AudioManager.GET_DEVICES_INPUTS).filter { it.type in usaveis }

    fun available(context: Context): List<InputOption> = devices(context).map { InputOption(it.id, label(it)) }

    fun deviceById(context: Context, id: Int): AudioDeviceInfo? = devices(context).firstOrNull { it.id == id }

    /** Entrada ativa: a escolhida (se ainda conectada); senao USB/fone com fio; senao o microfone do celular. */
    fun active(context: Context, preferredId: Int): InputOption {
        val all = devices(context)
        val chosen = all.firstOrNull { it.id == preferredId }
            ?: all.firstOrNull { it.type != AudioDeviceInfo.TYPE_BUILTIN_MIC && it.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            ?: all.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        return chosen?.let { InputOption(it.id, label(it)) } ?: InputOption(-1, "Microfone do celular")
    }

    private fun label(d: AudioDeviceInfo): String {
        val tipo = rotulos[d.type] ?: "Entrada"
        val nome = d.productName?.toString().orEmpty()
        return if (d.type == AudioDeviceInfo.TYPE_BUILTIN_MIC || nome.isBlank()) tipo else "$tipo ($nome)"
    }
}
