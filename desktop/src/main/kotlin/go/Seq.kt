package go

/** Мост gomobile к Android. На компьютере ядро - отдельный процесс, и мост не нужен. */
object Seq {
    @JvmStatic fun setContext(context: Any?) {}
}
