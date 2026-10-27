package vn.edu.nhansu.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import vn.edu.nhansu.R
import java.util.concurrent.Executors

/** Đọc ảnh thu nhỏ ở luồng nền, không nạp ảnh gốc dung lượng lớn vào danh sách. */
object PhotoLoader {
    private val worker = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(view: ImageView, uri: String, onFailure: (() -> Unit)? = null) {
        view.clipToOutline = true
        view.tag = uri
        view.setImageResource(R.drawable.ic_people)
        if (uri.isBlank()) return
        cache.get(uri)?.let { view.setImageBitmap(it); return }
        val resolver = view.context.applicationContext.contentResolver
        worker.execute {
            val bitmap = readThumbnail(resolver, Uri.parse(uri))
            if (bitmap != null) cache.put(uri, bitmap)
            main.post {
                // Một row có thể đã được tái sử dụng cho nhân sự khác.
                if (view.tag == uri && view.isAttachedToWindow) {
                    if (bitmap != null) view.setImageBitmap(bitmap) else onFailure?.invoke()
                }
            }
        }
    }

    private fun readThumbnail(resolver: ContentResolver, uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: Exception) {
        // Ảnh bị xóa hoặc quyền truy cập bị thu hồi: hiển thị avatar mặc định.
        null
    }
}
