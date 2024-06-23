package io.rewynd.android.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.ktor.utils.io.jvm.javaio.copyTo
import io.rewynd.client.RewyndClient
import java.io.ByteArrayOutputStream

suspend fun RewyndClient.loadBitmap(imageId: String): Bitmap? {
    val retrieved = getImage(imageId).body()
    val os = ByteArrayOutputStream()
    val copiedBytes = retrieved.copyTo(os)
    return BitmapFactory.decodeByteArray(os.toByteArray(), 0, copiedBytes.toInt())
}
