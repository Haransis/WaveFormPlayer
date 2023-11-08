package fr.haran.soundwave.controller

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

private const val DEFAULT_BUFFER_SIZE = 1024 * 64
private const val DEFAULT_CONNECT_TIMEOUT = 3000
private const val DEFAULT_READ_TIMEOUT = 3000
class CacheMediaPlayer(
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private var connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
    private var readTimeout: Int = DEFAULT_READ_TIMEOUT
) : MediaPlayer() {

    private var selector: Selector? = null
    private var serverChannel: ServerSocketChannel? = null
    private var proxyJob: Job? = null
    private var mOnErrorListener: OnErrorListener? = null
    var port = 0
    private val buffer = ByteBuffer.allocateDirect(bufferSize)
    private val bytes = ByteArray(buffer.capacity())
    var cacheDir: String? = null
    set(value) {
        field = value
        if (value != null)
            File(value).mkdirs()
    }

    init {
        try {
            selector = Selector.open()
            serverChannel = ServerSocketChannel.open()
            serverChannel?.let {
                it.socket().bind(null)
                port = it.socket().localPort
                it.configureBlocking(false)
                it.register(selector, SelectionKey.OP_ACCEPT)
            }
        } catch (e: IOException) {
            Timber.e("init: Proxy initialization failed.")
        }
    }

    override fun prepareAsync() {
        super.prepareAsync()
        proxyJob = CoroutineScope(Dispatchers.IO).launch { run() }
    }

    override fun release() {
        super.release()
        CoroutineScope(Dispatchers.Default).launch { close() }
    }

    private suspend fun close() {
        proxyJob?.cancelAndJoin()
        proxyJob = null
    }

    @Throws(IOException::class)
    override fun setDataSource(path: String) {
        super.setDataSource("http://127.0.0.1:$port/$path")
    }

    override fun setOnErrorListener(listener: OnErrorListener?) {
        super.setOnErrorListener(listener)
        mOnErrorListener = listener
    }

    private suspend fun run() {
        return withContext(Dispatchers.IO) {
            while (isActive) {
                try {
                    selector!!.select()
                    val selected = selector!!.selectedKeys()
                    if (selected.isEmpty()) {
                        continue
                    }
                    val it = selected.iterator()
                    while (it.hasNext()) {
                        val key = it.next()
                        if (key.isAcceptable) {
                            accept()
                        } else if (key.isReadable) {
                            process(key)
                        }
                    }
                    selected.clear()
                } catch (e: IOException) {
                    mOnErrorListener?.let { CoroutineScope(Dispatchers.Main).launch {
                        it.onError(this@CacheMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                    } }
                    cancel("proxy died", e)
                }
            }
            try {
                selector!!.close()
                serverChannel!!.close()
            } catch (e: IOException) {
                Timber.e("run: Proxy cleanup failed.")
            }
        }
    }

    @Throws(IOException::class)
    private fun accept() =
        serverChannel!!.accept()?.let {
            it.configureBlocking(false)
            it.register(selector, SelectionKey.OP_READ)
        }

    @Throws(IOException::class)
    private fun buildStringFromRequest(key: SelectionKey): String {
        val builder = StringBuilder()
        val socketChannel = key.channel() as SocketChannel
        buffer.clear()
        while (socketChannel.read(buffer) > 0) {
            buffer.flip()
            val dst = ByteArray(buffer.limit())
            buffer[dst]
            builder.append(String(dst))
            buffer.clear()
        }
        return builder.toString()
    }

    private fun buildResponseHeadersStream(connection: HttpURLConnection): ByteArrayInputStream {
        val protocol = connection.getHeaderField(null)
        val builder = StringBuilder()
        builder.append("$protocol\r\n")
        for (key in connection.headerFields.keys) {
            if (protocol != null)
                builder.append("$key: ${connection.getHeaderField(key)}\r\n")
        }
        builder.append("\r\n")
        return ByteArrayInputStream(builder.toString().toByteArray())
    }

    @Throws(IOException::class)
    private fun process(key: SelectionKey) {
        val request = Request(buildStringFromRequest(key))
        if (File("$cacheDir/${request.hash}").exists()) {
            write(
                key.channel() as SocketChannel,
                FileInputStream("$cacheDir/${request.hash}"),
                null
            )
        } else {
            try {
                (request.url!!.openConnection() as? HttpURLConnection?)?.let {
                    for (headerKey in request.headers.keys) {
                        it.setRequestProperty(headerKey, request.headers[headerKey])
                    }
                    it.connectTimeout = connectTimeout
                    it.readTimeout = readTimeout
                    it.connect()
                    val fos = FileOutputStream("$cacheDir/${request.hash}")
                    write(key.channel() as SocketChannel, buildResponseHeadersStream(it), fos)
                    write(key.channel() as SocketChannel, it.inputStream, fos)
                    fos.close()
                    it.disconnect()
                }
            } catch (e: Exception) {
                with(File("$cacheDir/${request.hash}")){
                    if (exists()) delete()
                }
                throw e
            }
        }
        key.channel().close()
        key.cancel()
    }

    private fun write(channel: SocketChannel, stream: InputStream, fos: FileOutputStream?) {
        var n: Int
        try {
            while (-1 != stream.read(bytes).also { n = it }) {
                fos?.write(bytes, 0, n)
                buffer.clear()
                buffer.put(bytes, 0, n)
                buffer.flip()
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
            }
        } catch (e: IOException) {
            Timber.e("write: Write to channel/cache failed.")
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                Timber.e("write: Could not close the stream.")
            }
        }
    }

    private fun error() {

    }

    private inner class Request(request: String) {
        val headers: HashMap<String, String>
        var url: URL? = null
        val hash: String

        fun md5(s: String): String {
            try {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()
                val hex = StringBuffer()
                for (i in messageDigest.indices) hex.append(
                    Integer.toHexString(
                        0xFF and messageDigest[i]
                            .toInt()
                    )
                )
                return hex.toString()
            } catch (e: NoSuchAlgorithmException) {
                Timber.wtf("md5: No md5 support. Results unpredictable.")
            }
            return ""
        }

        init {
            val builder = StringBuilder()
            headers = HashMap()
            for (line in request.split("\r\n")) {
                if (line.startsWith("GET")) {
                    url = try {
                        URL(line.split(' ', limit = 3)[1].substring(1))
                    } catch (e: MalformedURLException) {
                        Timber.e("init: CachedMediaPlayer data source URL is malformed.")
                        null
                    }
                    builder.append(line)
                } else {
                    val parts = line.split(':', limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        if (key != "Host") {
                            builder.append(value)
                        }
                        headers[key] = value
                    }
                }
            }
            hash = md5(builder.toString())
        }
    }
}