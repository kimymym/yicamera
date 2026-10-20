package com.github.kimymym.yicamera.data.model

enum class MediaType {
    PHOTO,
    VIDEO
}

data class CameraMediaItem(
    val name: String,
    val fullUrl: String,
    val thumbnailUrl: String,
    val screenUrl: String = "",
    val sizeBytes: Long = 0L,
    val dateTime: String = "",
    val durationSec: Long = 0L,
    val mediaType: MediaType = MediaType.PHOTO
) {
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO

    val formattedSize: String get() {
        if (sizeBytes <= 0) return ""
        val mb = sizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            val kb = sizeBytes / 1024.0
            String.format("%.1f KB", kb)
        }
    }

    val formattedDuration: String get() {
        val totalSec = if (durationSec > 0) {
            durationSec
        } else if (sizeBytes > 0) {
            // YI 카메라 1080p 평균 비트레이트 약 15Mbps (1.875MB/s) 기반 추정
            (sizeBytes / (1.875 * 1024 * 1024)).toLong().coerceAtLeast(1L)
        } else {
            0L
        }
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    /**
     * Ambarella 카메라 내장 비디오 JPEG 썸네일 (.THM) URL
     */
    val thmUrl: String
        get() = if (isVideo) {
            val baseName = name.substringBeforeLast(".")
            "http://192.168.42.1/DCIM/100MEDIA/$baseName.THM"
        } else {
            thumbnailUrl
        }

    val thmUrlLower: String
        get() = if (isVideo) {
            val baseName = name.substringBeforeLast(".")
            "http://192.168.42.1/DCIM/100MEDIA/$baseName.thm"
        } else {
            thumbnailUrl
        }
}

data class DownloadProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFileName: String = "",
    val currentFileProgress: Float = 0f,
    val isCompleted: Boolean = false,
    val error: String? = null
)
