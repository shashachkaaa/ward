package android.content

import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

abstract class BroadcastReceiver {
    abstract fun onReceive(context: Context?, intent: Intent?)
}

class IntentFilter() {
    internal val actions = mutableSetOf<String>()
    constructor(action: String) : this() { actions += action }
    fun addAction(action: String) { actions += action }
    fun hasAction(action: String?): Boolean = action in actions
}

/**
 * Доставка сообщений внутри процесса. Получатели зовутся в потоке окна, как на
 * Android в главном потоке: интерфейс меняет своё состояние прямо в onReceive.
 */
internal object Broadcasts {
    private val receivers = CopyOnWriteArrayList<Pair<BroadcastReceiver, IntentFilter>>()

    fun register(r: BroadcastReceiver, f: IntentFilter) { receivers += r to f }
    fun unregister(r: BroadcastReceiver) { receivers.removeIf { it.first === r } }

    fun send(context: Context, intent: Intent) {
        val targets = receivers.filter { it.second.hasAction(intent.action) }.map { it.first }
        if (targets.isEmpty()) return
        SwingUtilities.invokeLater { targets.forEach { it.onReceive(context, intent) } }
    }
}
