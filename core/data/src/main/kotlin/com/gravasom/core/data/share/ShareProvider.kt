package com.gravasom.core.data.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Compartilha via FileProvider com URI temporaria; nunca expoe caminho bruto. */
object ShareProvider {

    const val AUTHORITY_SUFFIX = ".fileprovider"

    fun intentFor(context: Context, file: File, mimeType: String): Intent {
        val authority = context.packageName + AUTHORITY_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
