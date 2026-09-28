package androidx.compose.ui.graphics

/** Растр в картинку Compose - как на Android. */
fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = image.toComposeImageBitmap()
