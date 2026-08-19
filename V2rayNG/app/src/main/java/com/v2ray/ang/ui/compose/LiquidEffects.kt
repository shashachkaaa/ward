package com.v2ray.ang.ui.compose

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Эффекты жидкого стекла, которым не нашлось места среди поверхностей: полоса
 * затухающего размытия внизу экрана и наклон телефона, за которым едет блик.
 */

/** Размытие с маской и блик по наклону требуют шейдеров - это Android 13. */
private val runtimeShadersAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Высота полосы размытия под нижней капсулой. */
val BottomBlurHeight = 148.dp

/**
 * Маска затухания: снизу размытие в полную силу, кверху сходит на нет.
 *
 * Цвет приходит с умноженной альфой, поэтому домножение на неё гасит и цвет, и
 * прозрачность разом - размытая копия растворяется в чётком содержимом под собой.
 */
private const val BottomFadeShader = """
uniform shader content;
uniform float2 size;

half4 main(float2 coord) {
    float fade = smoothstep(0.0, size.y, coord.y);
    return content.eval(coord) * fade;
}
"""

/**
 * Полоса затухающего размытия у нижнего края экрана.
 *
 * Список уезжает под нижнюю капсулу и обрывается о её край. Полоса кладёт поверх
 * размытую копию того же содержимого и гасит её кверху - строки растворяются под
 * капсулой, а не обрезаются.
 *
 * Ниже Android 13 не рисуется вовсе: маска - шейдер, а без неё осталось бы ровное
 * размытие с жёсткой границей сверху, что хуже, чем ничего.
 *
 * @param backdrop Слой с содержимым экрана.
 * @param height Высота полосы.
 * @param blurRadius Сила размытия у самого низа.
 */
@Composable
fun BottomBlurScrim(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    height: Dp = BottomBlurHeight,
    blurRadius: Dp = 16.dp
) {
    if (!runtimeShadersAvailable) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawPlainBackdrop(
                backdrop = backdrop.backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(blurRadius.toPx())
                    runtimeShaderEffect("BottomFade", BottomFadeShader, "content") {
                        setFloatUniform("size", size.width, size.height)
                    }
                }
            )
    )
}

/**
 * Угол силы тяжести в градусах - куда наклонён телефон.
 *
 * Настоящее стекло ловит свет под тем углом, под которым его держат, и блик по нему
 * едет вместе с наклоном. Отдаётся состоянием, а не значением: читать его нужно
 * внутри отрисовки, иначе перекомпоновка шла бы на каждое событие датчика.
 *
 * Датчик слушается только пока экран на виду.
 */
@Composable
fun rememberGravityAngle(): State<Float> {
    val context = LocalContext.current
    val holder = remember(context) { GravityAngleHolder.of(context) }

    LifecycleStartEffect(holder) {
        holder.acquire()
        onStopOrDispose { holder.release() }
    }

    return holder
}

/**
 * Наклон один на всё приложение.
 *
 * Стекло разошлось по десяткам мест, и заводить каждому свой слушатель датчика
 * нельзя: они все считали бы один и тот же угол, разряжая батарею в несколько рук.
 * Датчик слушается, пока его держит хоть кто-то.
 */
private class GravityAngleHolder(context: Context) : State<Float> {

    /** 45 градусов - то же положение блика, что у библиотеки без датчика. */
    override var value: Float by mutableFloatStateOf(DefaultAngle)
        private set

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            val angle = atan2(event.values[1], event.values[0]) * (180f / PI).toFloat()
            // Угол ходит по кругу, и на стыке -180 и 180 разность выходит почти в
            // полный оборот. Приводим её к короткой стороне, иначе блик на этом стыке
            // прокручивало бы вокруг всей кнопки
            var delta = angle - value
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            // Сглаживание: без него блик дрожит от каждого шороха в руке
            value += delta * Smoothing
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private var holders = 0

    @Synchronized
    fun acquire() {
        val sensor = accelerometer ?: return
        if (holders++ == 0) {
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    @Synchronized
    fun release() {
        if (accelerometer == null) return
        if (--holders <= 0) {
            holders = 0
            sensorManager?.unregisterListener(listener)
        }
    }

    companion object {
        private const val DefaultAngle = 45f
        private const val Smoothing = 0.5f

        @Volatile
        private var instance: GravityAngleHolder? = null

        fun of(context: Context): GravityAngleHolder =
            instance ?: synchronized(this) {
                instance ?: GravityAngleHolder(context).also { instance = it }
            }
    }
}
