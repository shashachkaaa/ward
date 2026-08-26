package com.v2ray.ang.root

/**
 * Ожидание конца процесса со сроком - своё, а не из стандартной библиотеки.
 *
 * У `Process` такой метод есть, но появился он в API 26, а приложение работает
 * с 24. На Android 7.0 и 7.1 вызов не находит метода и бросает `NoSuchMethodError`.
 * Это `Error`, а не `Exception`, и стоящий рядом `catch (e: Exception)` его не
 * ловит - обращение к root на этих версиях роняло приложение целиком.
 *
 * Способ простой: спрашиваем код возврата, пока процесс не ответит. Пока он жив,
 * `exitValue` бросает `IllegalThreadStateException` - это и есть «ещё не готов».
 */
private const val POLL_INTERVAL_MS = 50L

/**
 * Ждёт завершения процесса, но не дольше срока.
 *
 * @param timeoutMs Сколько ждать.
 * @return Успел ли процесс завершиться.
 */
internal fun Process.awaitFor(timeoutMs: Long): Boolean {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    while (true) {
        try {
            exitValue()
            return true
        } catch (_: IllegalThreadStateException) {
            if (System.nanoTime() >= deadline) return false
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }
}
