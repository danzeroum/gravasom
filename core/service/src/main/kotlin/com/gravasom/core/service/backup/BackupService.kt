package com.gravasom.core.service.backup

import android.content.Context
import com.gravasom.core.audio.engine.edit.EditCodec
import com.gravasom.core.data.backup.BackupCipher
import com.gravasom.core.data.db.EditProjectStore
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerColor
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.preset.FormatPreset
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.RecordingStorage
import com.gravasom.core.service.library.RecordingFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.MessageDigest

/** Resultado de criar um pacote. */
sealed interface BackupResult {
    data class Done(val recordings: Int, val bytes: Long) : BackupResult
    data class Failed(val reason: String) : BackupResult
}

/** Resultado de restaurar. [WrongPassword] nao toca em nada: nada restaurado, nada apagado. */
sealed interface RestoreResult {
    data class Done(val restored: Int) : RestoreResult
    data object WrongPassword : RestoreResult
    data class Failed(val reason: String) : RestoreResult
}

/** Grava uma gravacao restaurada na biblioteca e devolve o id (cola fina, trocavel nos testes). */
fun interface BackupSaver {
    suspend fun save(file: File, title: String, forcePrivate: Boolean, preset: FormatPreset?): Long?
}

/** Um marcador do pacote (a posicao e relativa a gravacao; o id novo sai do banco na restauracao). */
class BackupMarker(val positionMillis: Long, val label: String, val note: String, val color: String)

/** Metadados de uma gravacao do pacote: tudo o que nao esta nos bytes do audio. */
data class EntryMeta(
    val title: String,
    val format: String,
    val size: Long,
    val sha256: String,
    val favorite: Boolean,
    val tags: List<String>,
    val preset: String?,
    val private: Boolean,
    val markers: List<BackupMarker>,
    /** Projeto de efeitos (EditCodec, texto versionado), se a gravacao tinha um salvo. */
    val project: String?,
)

/** Uma gravacao decifrada e conferida, pronta para voltar a biblioteca. */
class RestoredEntry(val file: File, val meta: EntryMeta)

/**
 * Backup e restauracao cifrados da biblioteca inteira (Onda 2, "protecao"): UM pacote `.gravasom` com as gravacoes (bytes em
 * claro, cifrados em fluxo pelo Tink — do cofre sai decifrado), marcadores, etiquetas, favorito e projeto de efeitos de cada uma.
 *
 * Restaurar e em duas fases: primeiro decifra e confere o SHA-256 de TUDO numa pasta temporaria; so entao grava na biblioteca.
 * Senha errada (falha na abertura do envelope, antes de qualquer escrita) ou pacote corrompido deixam tudo como estava: nada
 * restaurado, nada apagado. Restaurar nunca exclui — so acrescenta gravacoes novas (ADR-008: nada destrutivo).
 */
class BackupService(
    private val context: Context,
    private val repo: RecordingRepository,
    private val files: RecordingFiles,
    private val markers: MarkerRepository,
    private val projects: EditProjectStore,
    private val saver: BackupSaver,
) {

    /** Cifra a biblioteca inteira em [output]; [onProgress] recebe (feito, total) por gravacao escrita. */
    suspend fun create(
        output: OutputStream,
        password: CharArray,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): BackupResult = withContext(Dispatchers.IO) {
        val recordings = repo.observeAll().first()
        val salt = BackupCipher.newSalt()
        val cipher = BackupCipher.newCipher()
        val wrapped = cipher.wrappedKeyset(BackupCipher.deriveKey(password, salt))
        var out: DataOutputStream? = null
        var bytes = 0L
        try {
            writeHeader(output, salt, wrapped)
            out = DataOutputStream(cipher.encrypting(output))
            for ((i, r) in recordings.withIndex()) {
                bytes += writeEntry(out, r)
                onProgress(i + 1, recordings.size)
            }
            writeEnd(out)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return@withContext BackupResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalStateException) {
            // ex.: "o arquivo mudou durante o backup" (check em writeEntry): falha com mensagem, sem derrubar o app.
            return@withContext BackupResult.Failed(e.message ?: "falha ao gerar o pacote")
        } finally {
            runCatching { out?.close() }
        }
        BackupResult.Done(recordings.size, bytes)
    }

    /** Restaura um pacote; [onProgress] recebe (feito, total), com total = -1 enquanto nao se sabe. */
    suspend fun restore(
        input: InputStream,
        password: CharArray,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): RestoreResult = withContext(Dispatchers.IO) {
        val cipher = try {
            readHeader(input, password)
        } catch (_: GeneralSecurityException) {
            return@withContext RestoreResult.WrongPassword
        } catch (_: IOException) {
            return@withContext RestoreResult.Failed("pacote ilegivel")
        } catch (_: RuntimeException) {
            return@withContext RestoreResult.Failed("pacote ilegivel")
        }
        val temp = File(context.cacheDir, "restaurar").apply { deleteRecursively(); mkdirs() }
        try {
            val entries = readEntries(cipher.decrypting(input), temp, onProgress)
            RestoreResult.Done(commit(entries))
        } catch (e: CancellationException) {
            throw e
        } catch (_: GeneralSecurityException) {
            RestoreResult.Failed("pacote adulterado")
        } catch (_: IOException) {
            RestoreResult.Failed("erro de E/S ao ler o pacote")
        } catch (_: JSONException) {
            RestoreResult.Failed("pacote corrompido")
        } catch (_: RuntimeException) {
            // check() do SHA-256, tamanhos invalidos etc.: um pacote ruim nunca pode derrubar o app nem restaurar pela metade.
            RestoreResult.Failed("pacote corrompido")
        } finally {
            temp.deleteRecursively()
        }
    }

    /** Cabecalho em claro: magica, versao, sal, iteracoes e o keyset do pacote envolvido pela senha. */
    private fun writeHeader(output: OutputStream, salt: BackupCipher.Salt, wrapped: ByteArray) {
        val raw = DataOutputStream(output)
        raw.write(MAGIC)
        raw.writeByte(VERSION)
        raw.writeInt(salt.bytes.size)
        raw.write(salt.bytes)
        raw.writeInt(salt.iterations)
        raw.writeInt(wrapped.size)
        raw.write(wrapped)
        raw.flush()
    }

    /** Le o cabecalho e devolve a cifra do pacote; [GeneralSecurityException] = senha errada (a tag nao confere). */
    private fun readHeader(input: InputStream, password: CharArray): BackupCipher {
        val raw = DataInputStream(input)
        val magic = ByteArray(MAGIC.size)
        raw.readFully(magic)
        if (!magic.contentEquals(MAGIC)) throw IOException("nao e um pacote do Gravasom")
        val version = raw.readByte().toInt()
        if (version != VERSION) throw IOException("versao de pacote desconhecida: $version")
        // O cabecalho vem de um arquivo nao confiavel e e lido ANTES da senha ser verificada: limita tudo.
        val salt = ByteArray(boundedInt(raw.readInt(), MIN_SALT, MAX_SALT, "sal"))
        raw.readFully(salt)
        val iterations = boundedInt(raw.readInt(), MIN_ITERATIONS, MAX_ITERATIONS, "iteracoes")
        val wrapped = ByteArray(boundedInt(raw.readInt(), MIN_WRAPPED, MAX_WRAPPED, "chave embrulhada"))
        raw.readFully(wrapped)
        return BackupCipher.open(wrapped, BackupCipher.deriveKey(password, BackupCipher.Salt(salt, iterations)))
    }

    /** Escreve uma gravacao (cabecalho JSON + bytes do audio) e devolve o tamanho escrito. */
    private suspend fun writeEntry(out: DataOutputStream, recording: Recording): Long =
        // Copia em claro de uso unico, apagada ao fim de CADA gravacao: o backup da biblioteca privada inteira nao deixa o claro no cache.
        files.withPlain(recording) { source ->
            if (!source.isFile) throw IOException("nao foi possivel ler ${recording.title}")
            val meta = metaOf(recording, source)
            val header = meta.toJson().toString().toByteArray(Charsets.UTF_8)
            out.writeInt(header.size)
            out.write(header)
            val written = copy(source, out)
            check(written == meta.size) { "o arquivo mudou durante o backup" }
            written
        }

    private fun writeEnd(out: DataOutputStream) {
        val header = JSONObject().put(KIND, KIND_END).toString().toByteArray(Charsets.UTF_8)
        out.writeInt(header.size)
        out.write(header)
    }

    /** Fase 1 da restauracao: decifra o pacote inteiro em [temp], conferindo o SHA-256 de cada arquivo. */
    private fun readEntries(
        input: InputStream,
        temp: File,
        onProgress: (Int, Int) -> Unit,
    ): List<RestoredEntry> {
        val entries = mutableListOf<RestoredEntry>()
        DataInputStream(input).use { stream ->
            while (true) {
                val meta = nextMeta(stream) ?: break
                val file = File(temp, "f${entries.size}.${meta.format.lowercase()}")
                val digest = MessageDigest.getInstance("SHA-256")
                file.outputStream().use { target ->
                    digestUpdateCopy(stream, target, meta.size, digest)
                }
                check(hex(digest.digest()) == meta.sha256) { "SHA-256 nao confere: ${meta.title}" }
                entries += RestoredEntry(file, meta)
                onProgress(entries.size, -1)
            }
        }
        return entries
    }

    /** Le o proximo cabecalho; null no fim do pacote. */
    private fun nextMeta(stream: DataInputStream): EntryMeta? {
        val header = ByteArray(boundedInt(stream.readInt(), 2, MAX_ENTRY_HEADER, "cabecalho da gravacao"))
        stream.readFully(header)
        val json = JSONObject(String(header, Charsets.UTF_8))
        if (json.optString(KIND) != KIND_FILE) return null
        val meta = metaFromJson(json)
        if (meta.size < 0) throw IOException("tamanho invalido no pacote")
        // O formato vira extensao de arquivo: so os conhecidos.
        if (RecordingFormat.entries.none { it.name == meta.format }) throw IOException("formato desconhecido no pacote")
        return meta
    }

    private fun boundedInt(value: Int, min: Int, max: Int, what: String): Int {
        if (value !in min..max) throw IOException("pacote invalido: $what fora do limite")
        return value
    }

    /** Fase 2: grava cada gravacao conferida como uma NOVA gravacao da biblioteca, com marcadores e etiquetas. */
    private suspend fun commit(entries: List<RestoredEntry>): Int {
        var restored = 0
        for ((i, e) in entries.withIndex()) {
            val target = File(
                RecordingStorage.dir(context),
                "restaurada_${System.currentTimeMillis()}_${i}.${e.meta.format.lowercase()}",
            )
            e.file.copyTo(target, overwrite = true)
            val preset = e.meta.preset?.let { name -> runCatching { FormatPreset.valueOf(name) }.getOrNull() }
            val id = saver.save(target, e.meta.title, e.meta.private, preset)
            if (id != null) {
                applyMeta(id, e.meta)
                restored++
            } else {
                target.delete()
            }
        }
        return restored
    }

    /** Marcadores, etiquetas, favorito e projeto voltam para a gravacao nova. */
    private suspend fun applyMeta(id: Long, meta: EntryMeta) {
        if (meta.favorite) repo.setFavorite(id, true)
        if (meta.tags.isNotEmpty()) repo.setTags(id, meta.tags)
        meta.markers.forEach { m ->
            markers.insert(
                Marker(
                    recordingId = id, positionMillis = m.positionMillis, label = m.label, note = m.note,
                    color = runCatching { MarkerColor.valueOf(m.color) }.getOrDefault(MarkerColor.AMBER),
                ),
            )
        }
        meta.project?.let { encoded ->
            runCatching { EditCodec.decode(encoded) }.getOrNull()?.let { projects.save(id, it) }
        }
    }

    private suspend fun metaOf(recording: Recording, source: File): EntryMeta = EntryMeta(
        title = recording.title,
        format = recording.format.name,
        size = source.length(),
        sha256 = sha256Of(source),
        favorite = recording.isFavorite,
        tags = recording.tags,
        preset = recording.preset?.name,
        private = recording.isPrivate,
        markers = markers.list(recording.id).map { BackupMarker(it.positionMillis, it.label, it.note, it.color.name) },
        project = projects.load(recording.id)?.let { EditCodec.encode(it) },
    )

    private fun sha256Of(source: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        source.inputStream().use { input ->
            val buf = ByteArray(COPY_BUFFER)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return hex(digest.digest())
    }

    /** Copia [size] bytes de [stream] para [target] atualizando [digest]; devolve o total copiado. */
    private fun digestUpdateCopy(stream: InputStream, target: OutputStream, size: Long, digest: MessageDigest): Long {
        var done = 0L
        val buf = ByteArray(COPY_BUFFER)
        while (done < size) {
            val n = stream.read(buf, 0, minOf(buf.size.toLong(), size - done).toInt())
            if (n < 0) throw IOException("pacote curto")
            digest.update(buf, 0, n)
            target.write(buf, 0, n)
            done += n
        }
        return done
    }

    /** Copia um arquivo para [out]; devolve o total copiado. */
    private fun copy(source: File, out: OutputStream): Long {
        var done = 0L
        source.inputStream().use { input ->
            val buf = ByteArray(COPY_BUFFER)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                done += n
            }
        }
        return done
    }

    private fun EntryMeta.toJson(): JSONObject = JSONObject().apply {
        put(KIND, KIND_FILE)
        put("title", title)
        put("format", format)
        put("size", size)
        put("sha256", sha256)
        put("favorite", favorite)
        put("tags", JSONArray(tags))
        put("preset", preset ?: JSONObject.NULL)
        put("private", private)
        put("markers", JSONArray().also { arr -> markers.forEach { arr.put(markerToJson(it)) } })
        put("project", project ?: JSONObject.NULL)
    }

    private fun metaFromJson(json: JSONObject): EntryMeta {
        val markerArray = json.optJSONArray("markers") ?: JSONArray()
        val markers = (0 until markerArray.length()).map { i ->
            val m = markerArray.getJSONObject(i)
            BackupMarker(m.getLong("positionMillis"), m.optString("label"), m.optString("note"), m.optString("color"))
        }
        val tags = json.optJSONArray("tags") ?: JSONArray()
        return EntryMeta(
            title = json.getString("title"),
            format = json.getString("format"),
            size = json.getLong("size"),
            sha256 = json.getString("sha256"),
            favorite = json.optBoolean("favorite"),
            tags = (0 until tags.length()).map { tags.getString(it) },
            preset = json.optString("preset").takeUnless { it.isBlank() || it == "null" },
            private = json.optBoolean("private"),
            markers = markers,
            project = json.optString("project").takeUnless { it.isBlank() || it == "null" },
        )
    }

    private fun markerToJson(m: BackupMarker): JSONObject = JSONObject()
        .put("positionMillis", m.positionMillis)
        .put("label", m.label)
        .put("note", m.note)
        .put("color", m.color)

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    companion object {
        private val MAGIC = byteArrayOf('G'.code.toByte(), 'S'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
        private const val VERSION = 1
        private const val MIN_SALT = 8
        private const val MAX_SALT = 64
        private const val MIN_ITERATIONS = 100_000
        private const val MAX_ITERATIONS = 2_000_000
        private const val MIN_WRAPPED = 28 // IV de 12 bytes + tag GCM de 16
        private const val MAX_WRAPPED = 4_096
        private const val MAX_ENTRY_HEADER = 1 shl 20
        private const val KIND = "kind"
        private const val KIND_FILE = "file"
        private const val KIND_END = "end"
        private const val COPY_BUFFER = 1 shl 16
    }
}
