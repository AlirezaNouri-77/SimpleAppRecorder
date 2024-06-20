package com.shermanrex.recorderApp.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.shermanrex.recorderApp.data.model.RecordModel
import com.shermanrex.recorderApp.data.util.getFileFormat
import com.shermanrex.recorderApp.data.util.removeFileformat
import com.shermanrex.recorderApp.domain.StorageManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageManager @Inject constructor(
  var context: Context,
) : StorageManagerImpl {

  override var currentFileUri: Uri = Uri.EMPTY

  override fun getSavePath(fileName: String, savePath: String): ParcelFileDescriptor? {
    val document = DocumentFile.fromTreeUri(
      context,
      Uri.parse(savePath)
    )
    val documentFile = document?.createFile("audio/*", fileName)
    val fileDescriptor = context.contentResolver.openFileDescriptor(documentFile!!.uri, "w")
    currentFileUri = documentFile.uri
    return fileDescriptor
  }

  override suspend fun deleteRecord(uri: Uri) {
    withContext(Dispatchers.IO) {
      val document = DocumentFile.fromSingleUri(context, uri)
      document?.delete() ?: false
    }
  }

  override suspend fun renameRecord(uri: Uri, newName: String): Uri = withContext(Dispatchers.IO) {
    DocumentsContract.renameDocument(context.contentResolver, uri, newName) ?: Uri.EMPTY
  }

  override suspend fun getFileDetailByMediaMetaRetriever(
    document: DocumentFile?,
  ): RecordModel? {

    val mediaMeta = MediaMetadataRetriever()

    return withContext(Dispatchers.IO) {
      mediaMeta.setDataSource(context, document?.uri)
      val duration =
        mediaMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
      val bitrate = mediaMeta.extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_BITRATE
      )?.toInt() ?: 0
      val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        mediaMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
          ?.toInt() ?: 0
      } else {
        0
      }
      return@withContext if (duration > 500 && document != null) {
        RecordModel(
          path = document.uri,
          fullName = document.name!!,
          name = document.name!!.removeFileformat(),
          format = document.name!!.getFileFormat(),
          duration = duration,
          bitrate = bitrate,
          size = document.length(),
          sampleRate = sampleRate,
        )
      } else null
    }
  }

}