package com.serortech.audioo.drive

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class UploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME) ?: return Result.failure()
        val uri = Uri.parse(uriStr)

        val account = DriveAuth.lastAccount(applicationContext)
        if (account == null) {
            Log.w(TAG, "no signed-in account, will retry: $name")
            return Result.retry()
        }

        return try {
            val uploader = DriveUploader(applicationContext, account)
            val driveId = uploader.upload(uri, name)
            Log.i(TAG, "uploaded $name as Drive id=$driveId, deleting local")
            try {
                applicationContext.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "local delete failed for $uri", e)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "upload failed for $name", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AudiooUploadWorker"
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"

        fun enqueue(ctx: Context, uri: Uri, name: String) {
            val data = Data.Builder()
                .putString(KEY_URI, uri.toString())
                .putString(KEY_NAME, name)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueue(request)
        }
    }
}
