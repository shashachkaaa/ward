package com.v2ray.ang.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Приведение фирменного цвета сервиса к нашей палитре.
 *
 * Главное здесь не разбор шестнадцатеричных знаков, а то, что владелец сервиса не
 * может прислать нечитаемое. Он задаёт тон, светлоту задаём мы - и проверяется
 * именно это: и чёрный, и белый выходят из функции нормальным цветом, на котором
 * видно буквы. Без такой проверки первый же присланный белый растворил бы кромку
 * на светлой теме, и заметили бы мы это по жалобе.
 */
class ServiceColorTest {

    private fun lightnessOf(color: Color): Float {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        return hsl[2]
    }

    @Test
    fun `разбирает цвет с решёткой и без`() {
        assertNotNull(serviceColor("#10B981", dark = true))
        assertNotNull(serviceColor("10B981", dark = true))
    }

    @Test
    fun `короткая запись раскрывается как полная`() {
        assertEquals(
            serviceColor("#11BB99", dark = true),
            serviceColor("#1B9", dark = true)
        )
    }

    @Test
    fun `прозрачность в записи отбрасывается, а не ломает разбор`() {
        assertEquals(
            serviceColor("#10B981", dark = true),
            serviceColor("#FF10B981", dark = true)
        )
    }

    @Test
    fun `мусор не разбирается`() {
        assertNull(serviceColor("зелёный", dark = true))
        assertNull(serviceColor("#12345", dark = true))
        assertNull(serviceColor("", dark = true))
        assertNull(serviceColor("#GGGGGG", dark = true))
    }

    @Test
    fun `белый не растворяется на светлой теме`() {
        val color = serviceColor("#FFFFFF", dark = false)
        assertNotNull(color)
        assertTrue("светлота должна быть нашей, а не присланной", lightnessOf(color!!) < 0.6f)
    }

    @Test
    fun `чёрный не пропадает на тёмной теме`() {
        val color = serviceColor("#000000", dark = true)
        assertNotNull(color)
        assertTrue("светлота должна быть нашей, а не присланной", lightnessOf(color!!) > 0.5f)
    }

    @Test
    fun `светлота не зависит от присланной, только от темы`() {
        val fromDark = serviceColor("#001100", dark = true)!!
        val fromBright = serviceColor("#CCFFCC", dark = true)!!
        assertEquals(lightnessOf(fromDark), lightnessOf(fromBright), 0.01f)
    }
}
