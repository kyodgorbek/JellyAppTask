package com.yodgorbek.jellyapp.domain.usacase


import com.yodgorbek.jellyapp.data.repository.CameraRepository
import java.io.File


class RecordVideoUseCase(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(videoFile: File): Result<String> {
        return repository.recordDualCameraVideo(videoFile)
    }
}