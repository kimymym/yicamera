package com.github.kimymym.yicamera.data.socket

import android.util.Log
import com.github.kimymym.yicamera.data.model.YiCommand
import com.github.kimymym.yicamera.data.model.YiMsgIds
import com.github.kimymym.yicamera.data.model.YiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YiSocketClient @Inject constructor() {
    private val TAG = "YiSocketClient"
    private val CAMERA_HOST = "192.168.42.1"
    private val CAMERA_PORT = 7878
    private val TIMEOUT_MS = 5000

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    private var sessionToken: Int = 0
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private val sendMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _responseFlow = MutableSharedFlow<YiResponse>(extraBufferCapacity = 64)
    val responseFlow: SharedFlow<YiResponse> = _responseFlow.asSharedFlow()

    private val pendingResponses = mutableMapOf<Int, Channel<YiResponse>>()
    private val pendingMutex = Mutex()

    /**
     * TCP 7878 소켓 연결
     */
    suspend fun connect(scope: CoroutineScope): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            Log.d(TAG, "Connecting to $CAMERA_HOST:$CAMERA_PORT...")
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(CAMERA_HOST, CAMERA_PORT), TIMEOUT_MS)
            newSocket.soTimeout = 0 // 비동기 읽기 루프를 위해 무한 타임아웃
            newSocket.tcpNoDelay = true

            socket = newSocket
            writer = OutputStreamWriter(newSocket.getOutputStream(), Charsets.UTF_8)
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream(), Charsets.UTF_8))

            // 응답 수신 루프 시작
            startReaderLoop(scope)
            Log.d(TAG, "Socket connected successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection failed", e)
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * 세션 시작 및 토큰 발급 (msg_id: 257)
     */
    suspend fun startSession(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cmd = YiCommand(msgId = YiMsgIds.MSG_START_SESSION, token = 0)
            val response = sendCommand(cmd, timeoutMs = 5000)
            if (response != null && response.rval == 0) {
                val token = response.param?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                sessionToken = token
                Log.d(TAG, "Session established with token: $token")
                Result.success(token)
            } else {
                Result.failure(Exception("Failed to start session: rval=${response?.rval}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session", e)
            Result.failure(e)
        }
    }

    /**
     * 명령 전송 및 특정 msg_id에 대한 응답 대기
     */
    suspend fun sendCommand(
        cmd: YiCommand,
        timeoutMs: Long = 4000
    ): YiResponse? = withContext(Dispatchers.IO) {
        val channel = Channel<YiResponse>(1)
        pendingMutex.withLock {
            pendingResponses[cmd.msgId] = channel
        }

        try {
            sendMutex.withLock {
                val payload = json.encodeToString(cmd)
                Log.d(TAG, "TX -> $payload")
                writer?.apply {
                    write(payload)
                    flush()
                } ?: throw IllegalStateException("Socket writer is null")
            }

            // 응답 대기 (타임아웃 적용)
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                channel.receive()
            }
        } finally {
            pendingMutex.withLock {
                pendingResponses.remove(cmd.msgId)
            }
        }
    }

    fun getSessionToken(): Int = sessionToken

    private fun startReaderLoop(scope: CoroutineScope) {
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = CharArray(4096)
            val stringBuilder = StringBuilder()

            try {
                while (isActive) {
                    val currentReader = reader ?: break
                    val count = currentReader.read(buffer)
                    if (count == -1) {
                        Log.w(TAG, "Socket reached EOF")
                        break
                    }
                    stringBuilder.append(buffer, 0, count)

                    // 여러 JSON 메시지가 붙어서 오거나 나뉘어 올 수 있으므로 분리 파싱
                    while (true) {
                        val fullString = stringBuilder.toString()
                        val startIndex = fullString.indexOf('{')
                        if (startIndex == -1) {
                            stringBuilder.clear()
                            break
                        }
                        val endIndex = findClosingBrace(fullString, startIndex)
                        if (endIndex == -1) {
                            // 아직 완전한 JSON이 도착하지 않음
                            break
                        }

                        val jsonChunk = fullString.substring(startIndex, endIndex + 1)
                        stringBuilder.delete(0, endIndex + 1)

                        Log.d(TAG, "RX <- $jsonChunk")
                        try {
                            val resp = json.decodeFromString<YiResponse>(jsonChunk)
                            _responseFlow.emit(resp)

                            // 대기 중인 채널에 전달
                            pendingMutex.withLock {
                                pendingResponses[resp.msgId]?.trySend(resp)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing JSON response: $jsonChunk", e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Socket reader loop terminated with exception", e)
                }
            }
        }
    }

    private fun findClosingBrace(str: String, startIndex: Int): Int {
        var depth = 0
        for (i in startIndex until str.length) {
            if (str[i] == '{') depth++
            else if (str[i] == '}') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    fun disconnect() {
        readJob?.cancel()
        heartbeatJob?.cancel()
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        } finally {
            socket = null
            writer = null
            reader = null
            sessionToken = 0
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true && socket?.isClosed == false
}
