package com.v2ray.ang.ui.compose

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

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
    // Полоса во всю ширину с размытием и своей программой для видеоядра - на каждом
    // кадре. На упрощённом уровне это первое, чем стоит пожертвовать
    if (LocalGlassQuality.current != GlassQuality.FULL) return

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
 * Угол блика на стекле. Настольная версия: у монитора нет наклона, и свет стоит
 * там же, где у библиотеки без датчика, - 45 градусов.
 */
@Composable
fun rememberGravityAngle(): State<Float> = remember { mutableFloatStateOf(45f) }
