package androidx.annotation

@Retention(AnnotationRetention.SOURCE) annotation class StringRes
@Retention(AnnotationRetention.SOURCE) annotation class DrawableRes
@Retention(AnnotationRetention.SOURCE) annotation class ArrayRes
@Retention(AnnotationRetention.SOURCE) annotation class ColorInt
@Retention(AnnotationRetention.SOURCE) annotation class WorkerThread
@Retention(AnnotationRetention.SOURCE) annotation class MainThread
@Retention(AnnotationRetention.SOURCE) annotation class Keep
@Retention(AnnotationRetention.SOURCE) annotation class VisibleForTesting
@Retention(AnnotationRetention.SOURCE) annotation class RequiresApi(val value: Int = 0, val api: Int = 0)
