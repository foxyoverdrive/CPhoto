package com.genius.cphoto.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import com.genius.cphoto.CRPhoto.Companion.IMAGE_SIZE
import java.io.IOException

/**
 * Created by Genius on 03.12.2017.
 */
class CRUtils {

    companion object {

        @Throws(IOException::class)
        private fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String?): Bitmap {
            val ei = ExifInterface(image_absolute_path ?: throw IllegalStateException("Image path is null"))

            return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(bitmap, horizontal = true, vertical = false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(bitmap, horizontal = false, vertical = true)
                else -> bitmap
            }
        }

        /**
         * Adding multiple intents of camera and gallery
         * @param context - current context
         * @param list - List<Intent> for receiving incoming Intents
         * @param intent - Intent for receive
        </Intent> */
        fun addIntentsToList(context: Context, list: MutableList<Intent>, intent: Intent): MutableList<Intent> {
            val resInfo = context.packageManager.queryIntentActivities(intent, 0)
            for (resolveInfo in resInfo) {
                val packageName = resolveInfo.activityInfo.packageName
                val targetedIntent = Intent(intent)
                targetedIntent.`package` = packageName
                list.add(targetedIntent)
            }
            return list
        }

        /**
         * Is external storage available for write
         * @return - is available
         */
        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        @Throws(IOException::class)
        fun getBitmap(context: Context, uri: Uri, width: Int?, height: Int?): Bitmap {
            var path: String?
            path = try {
                CRFileUtils.getPath(context, uri) //from Gallery
            } catch (e: Exception) {
                null
            }

            if (path == null)
                path = uri.path

            val iOptions = BitmapFactory.Options()
            iOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, iOptions)

            iOptions.inSampleSize = calculateInSampleSize(iOptions, width ?: IMAGE_SIZE, height ?: IMAGE_SIZE)
            iOptions.inJustDecodeBounds = false

            val original = BitmapFactory.decodeFile(path, iOptions)

            return modifyOrientation(original, path)
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {

                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }
    }
}