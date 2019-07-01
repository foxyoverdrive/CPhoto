package com.genius.cphoto

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.annotation.StringRes
import android.util.Pair
import androidx.annotation.StringDef
import androidx.fragment.app.FragmentActivity
import com.genius.cphoto.exceptions.CancelOperationException
import com.genius.cphoto.shared.TypeRequest
import com.genius.cphoto.util.CRFileUtils
import com.genius.cphoto.util.CRUtils
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import kotlin.collections.ArrayList

/**
 * Created by Genius on 03.12.2017.
 */
@Suppress("UNUSED")
class CRPhoto(private val context: Context) {

    private var bitmapSizes: Pair<Int, Int>? = null
    private var bitmapPublishSubject: CompletableDeferred<Bitmap>? = null
    private var uriPublishSubject: CompletableDeferred<Uri>? = null
    private var pathPublishSubject: CompletableDeferred<String>? = null
    private var bitmapMultiPublishSubject: CompletableDeferred<List<Bitmap>>? = null
    private var uriMultiPublishSubject: CompletableDeferred<List<Uri>>? = null
    private var pathMultiPublishSubject: CompletableDeferred<List<String>>? = null
    private lateinit var response: String

    var title: String? = null
        private set

    /**
     * Generic request for
     * @param typeRequest - selected source for bitmap
     * @return - observable that emits bitmaps
     */
    @Throws(CancelOperationException::class)
    suspend fun requestBitmap(@TypeRequest typeRequest: String): Bitmap {
        return requestBitmap(typeRequest, Pair(IMAGE_SIZE, IMAGE_SIZE))
    }

    /**
     * Request for single bitmap with explicitly set of size
     * @param typeRequest - selected source for bitmap
     * @param width - width of resized bitmap
     * @param height - height of resized bitmap
     * @return - observable that emits single bitmap
     */
    @Throws(CancelOperationException::class)
    suspend fun requestBitmap(@TypeRequest typeRequest: String, width: Int, height: Int): Bitmap {
        return requestBitmap(typeRequest, Pair(width, height))
    }

    /**
     * Request for list of bitmaps with default (1024) size
     * @return - observable tah emits list of scaled bitmaps
     */
    @Throws(CancelOperationException::class)
    suspend fun requestMultiBitmap(): List<Bitmap> {
        return requestMultiBitmap(Pair(IMAGE_SIZE, IMAGE_SIZE))
    }

    /**
     * Request for list of bitmaps with explicitly set of size
     * @param width - width of resized bitmaps
     * @param height - height of resized bitmaps
     * @return - observable that emits list of bitmaps
     */
    @Throws(CancelOperationException::class)
    suspend fun requestMultiBitmap(width: Int, height: Int): List<Bitmap> {
        return requestMultiBitmap(Pair(width, height))
    }

    /**
     * Generic request for getting bitmap observable
     * @param typeRequest - selected source for emitter
     * @param bitmapSize - requested bitmap scale size
     * @return - explicitly scaled or not (1024 by default) bitmap
     */
    @Throws(CancelOperationException::class)
    suspend fun requestBitmap(@TypeRequest typeRequest: String, bitmapSize: Pair<Int, Int>): Bitmap {
        response = BITMAP
        startOverlapFragment(typeRequest)
        this.bitmapSizes = bitmapSize
        bitmapPublishSubject = CompletableDeferred()
        return (bitmapPublishSubject as CompletableDeferred<Bitmap>).await()
    }

    /**
     * Request for single uri
     * @param typeRequest - selected source for emitter
     * @return - observable that emits a single uri
     */
    @Throws(CancelOperationException::class)
    @Deprecated(message = "Because Google Photo Content Provider forbids the use of it ury in other contexts, in addition, from which the call was made")
    suspend fun requestUri(@TypeRequest typeRequest: String): Uri {
        response = URI
        startOverlapFragment(typeRequest)
        uriPublishSubject = CompletableDeferred()
        return (uriPublishSubject as CompletableDeferred<Uri>).await()
    }

    /**
     * Request for single path of file
     * @param typeRequest - selected source for emitter
     * @return - observable that emits a single path
     */
    @Throws(CancelOperationException::class)
    suspend fun requestPath(@TypeRequest typeRequest: String): String {
        response = PATH
        startOverlapFragment(typeRequest)
        pathPublishSubject = CompletableDeferred()
        return (pathPublishSubject as CompletableDeferred<String>).await()
    }

    /**
     * Request for list of bitmaps with explicitly set of size
     * @param bitmapSize - requested bitmap scale size
     * @return - explicitly scaled or not (1024 by default) bitmap
     */
    @Throws(CancelOperationException::class)
    suspend fun requestMultiBitmap(bitmapSize: Pair<Int, Int>): List<Bitmap> {
        response = BITMAP
        startOverlapFragment(TypeRequest.COMBINE_MULTIPLE)
        this.bitmapSizes = bitmapSize
        bitmapMultiPublishSubject = CompletableDeferred()
        return (bitmapMultiPublishSubject as CompletableDeferred<List<Bitmap>>).await()
    }

    /**
     * Request for list of uris
     * @return - observable that emits a list of uris
     */
    @Throws(CancelOperationException::class)
    @Deprecated(message = "Because Google Photo Content Provider forbids the use of it ury in other contexts, in addition, from which the call was made")
    suspend fun requestMultiUri(): List<Uri> {
        response = URI
        startOverlapFragment(TypeRequest.COMBINE_MULTIPLE)
        uriMultiPublishSubject = CompletableDeferred()
        return (uriMultiPublishSubject as CompletableDeferred<List<Uri>>).await()
    }

    /**
     * Request for list of paths
     * @return - observable that emits a list of paths
     */
    @Throws(CancelOperationException::class)
    suspend fun requestMultiPath(): List<String> {
        response = PATH
        startOverlapFragment(TypeRequest.COMBINE_MULTIPLE)
        pathMultiPublishSubject = CompletableDeferred()
        return (pathMultiPublishSubject as CompletableDeferred<List<String>>).await()
    }

    /**
     * Adding title to intent chooser on string
     * @param title - title in string
     * @return - parent class
     */
    fun titleCombine(title: String): CRPhoto {
        this.title = title
        return this
    }

    /**
     * Adding title to intent chooser on resource id
     * @param titleId - title in resources id
     * @return - parent class
     */
    fun titleCombine(@StringRes titleId: Int): CRPhoto {
        this.title = context.getString(titleId)
        return this
    }

    /**
     * Start fragment for action
     * Calling [OverlapFragment.newInstance] with selected type request and CRPhoto instance
     * @param typeRequest - selected request
     */
    private fun startOverlapFragment(@TypeRequest typeRequest: String) {
        (context as? FragmentActivity)?.let {  activity ->
            activity.supportFragmentManager.findFragmentByTag(OverlapFragment.TAG)?.let { overlapFragment ->
                (overlapFragment as? OverlapFragment)?.newRequest(typeRequest, this)
            } ?: activity.supportFragmentManager.beginTransaction()
                .add(OverlapFragment.newInstance(typeRequest, this), OverlapFragment.TAG)
                .commit()
        } ?: propagateThrowable(ClassCastException("Attached context is not FragmentActivity"))
    }

    /**
     * Get the bitmap from the source by URI
     * @param uri - uri source
     * @return image in bitmap
     */
    @Throws(IOException::class)
    private fun getBitmapFromStream(uri: Uri): Bitmap? {
        return CRUtils.getBitmap(context, uri, bitmapSizes?.first, bitmapSizes?.second)
    }

    /**
     * Processing the result of selecting images by the user
     * @param uri - single uri of selected image
     */
    internal fun onActivityResult(uri: Uri?) {
        uri?.let {
            propagateResult(it)
        }
    }

    /**
     *Processing the results of selecting images by the user
     * @param uri - list of uris of selected images
     */
    internal fun onActivityResult(uri: List<Uri>) {
        propagateMultipleResult(uri)
    }

    /**
     * Handle throwable from fragment
     * @param error - throwable
     */
    internal fun propagateThrowable(error: Throwable) {
        when (response) {
            BITMAP -> {
                bitmapMultiPublishSubject?.completeExceptionally(error)
                bitmapPublishSubject?.completeExceptionally(error)
            }
            URI -> {
                uriMultiPublishSubject?.completeExceptionally(error)
                uriPublishSubject?.completeExceptionally(error)
            }
            PATH -> {
                pathMultiPublishSubject?.completeExceptionally(error)
                pathPublishSubject?.completeExceptionally(error)
            }
        }
    }

    /**
     * Handle result from fragment
     * @param uri - uri-result
     */
    private fun propagateResult(uri: Uri) {
        try {
            when (response) {
                BITMAP -> propagateBitmap(uri)
                URI -> propagateUri(uri)
                PATH -> propagatePath(uri)
            }
        } catch (e: Exception) {
            uriPublishSubject?.completeExceptionally(e)
            bitmapPublishSubject?.completeExceptionally(e)
            pathPublishSubject?.completeExceptionally(e)
        }
    }

    /**
     * Handle multiple result from fragment
     * @param uris - uris items from fragment
     */
    private fun propagateMultipleResult(uris: List<Uri>) {
        try {
            when (response) {
                BITMAP -> propagateMultipleBitmap(uris)
                URI -> propagateMultipleUri(uris)
                PATH -> propagateMultiplePaths(uris)
            }
        } catch (e: Exception) {
            uriPublishSubject?.completeExceptionally(e)
            bitmapPublishSubject?.completeExceptionally(e)
            pathPublishSubject?.completeExceptionally(e)
        }
    }

    /**
     * Handle single result from fragment
     * @param uri - uri item from fragment
     */
    private fun propagateUri(uri: Uri) {
        uriPublishSubject?.complete(uri)
    }

    /**
     * Handle single result from fragment
     * @param uri - uri item from fragment
     */
    private fun propagatePath(uri: Uri) {
        pathPublishSubject?.complete(CRFileUtils.getPath(context, uri))
    }

    /**
     * Handle multiple result from fragment
     * @param uris - uris items from fragment
     */
    private fun propagateMultipleUri(uris: List<Uri>) {
        uriMultiPublishSubject?.complete(uris)
    }

    /**
     * Handle result list of paths from fragment
     * @param uris - uris of path image fragment
     */
    private fun propagateMultiplePaths(uris: List<Uri>) {
        pathMultiPublishSubject?.let { continuation ->
            continuation.complete(uris.map { uri -> CRFileUtils.getPath(context, uri) } )
        }
    }

    /**
     * Handle single result bitmap from fragment
     * @param uriBitmap - uri for bitmap image fragment
     */
    private fun propagateBitmap(uriBitmap: Uri) {
        getBitmapFromStream(uriBitmap)?.let {
            bitmapPublishSubject?.complete(it)
        } ?: bitmapMultiPublishSubject?.completeExceptionally(IllegalStateException("Bitmap is null"))

    }

    /**
     * Handle result list of bitmaps from fragment
     * @param uris - uris of bitmap image fragment
     */
    private fun propagateMultipleBitmap(uris: List<Uri>) {
        val list = ArrayList<Bitmap>()

        for (item in uris) {
            val tmp = getBitmapFromStream(item)
            if (tmp != null) {
                list.add(tmp)
            }
        }

        bitmapMultiPublishSubject?.complete(list)
    }

    @StringDef(BITMAP, URI, PATH)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class ResponseType

    companion object {
        private const val BITMAP = "BITMAP"
        private const val URI = "URI"
        private const val PATH = "PATH"

        const val IMAGE_SIZE = 1024
        const val REQUEST_ATTACH_IMAGE = 9123
        const val REQUEST_TAKE_PICTURE = 9124
        const val REQUEST_COMBINE = 9125
        const val REQUEST_COMBINE_MULTIPLE = 9126
        const val REQUEST_DOCUMENT = 9127
        const val REQUEST_TYPE_EXTRA = "request_type_extra"
    }
}

/**
 * Get thumbnails bitmap for selected scale from source
 * @param resizeValues - pair values with requested size for bitmap
 * @return - scaled bitmap
 */
@Suppress("UNUSED")
infix fun Bitmap.toThumb(resizeValues: Pair<Int, Int>): Bitmap {
    return ThumbnailUtils.extractThumbnail(this, resizeValues.first, resizeValues.second)
}
@Suppress("UNUSED")
suspend infix fun Context.takePhotoPath(@TypeRequest typeRequest: String): String {
    return CRPhoto(this).requestPath(typeRequest)
}
@Suppress("UNUSED")
suspend infix fun Context.takePhotoBitmap(@TypeRequest typeRequest: String): Bitmap {
    return CRPhoto(this).requestBitmap(typeRequest)
}

@Suppress("DEPRECATION")
@Deprecated(message = "Because Google Photo Content Provider forbids the use of it ury in other contexts, in addition, from which the call was made")
suspend infix fun Context.takePhotoUri(@TypeRequest typeRequest: String): Uri {
    return CRPhoto(this).requestUri(typeRequest)
}