package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Каркас обычного экрана приложения: живой фон, стекло и полоса затухания снизу.
 *
 * Появился потому, что второстепенные экраны остались от v2rayNG со сплошной серой
 * заливкой и выглядели вставками из другого приложения. Держать эту сборку в каждом
 * экране отдельно значит однажды снова разойтись, поэтому она здесь одна на всех.
 *
 * Слоёв два, и это не прихоть: в общий слой пишется весь экран, и стекло внутри
 * содержимого рисовать его не вправе - оно само в него пишется. Поэтому фон пишется
 * во второй слой, и карточки преломляют именно его.
 *
 * @param title Заголовок в шапке.
 * @param onBackClick Возврат назад.
 * @param isLoading Показывать ли полоску занятости в шапке.
 * @param isSearchActive Заменён ли заголовок строкой поиска.
 * @param searchQuery Что набрано в строке поиска.
 * @param onSearchQueryChange Набор в строке поиска.
 * @param onSearchClose Закрытие строки поиска.
 * @param searchPlaceholder Подсказка в пустой строке поиска.
 * @param actions Кнопки справа в шапке.
 * @param floatingActionButton Кнопка у нижнего края. Стоит поверх полосы затухания,
 *   а не под ней: полоса размывает всё, что под неё попало, и кнопка внутри неё
 *   расплылась бы вместе со списком.
 * @param bottomFade Растворять ли содержимое у нижнего края. Списку это идёт,
 *   а редактору текста нет: полоса размоет последние строки, и править их придётся
 *   вслепую.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    searchPlaceholder: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomFade: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val backdrop = rememberGlassBackdrop()
    val contentBackdrop = rememberGlassBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        // Слой фона отдаётся содержимому: без него стеклянные карточки внутри экрана
        // не найдут, что преломлять, и молча станут сплошными
        CompositionLocalProvider(LocalContentBackdrop provides contentBackdrop) {
            Scaffold(
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                containerColor = Color.Transparent,
                // Цвет текста задаётся явно. Scaffold выводит его из цвета контейнера,
                // а для прозрачного вывести нечего - выходит Unspecified, и всякий
                // Text без своего цвета рисуется чёрным. На тёмной теме это чёрным
                // по чёрному: так пропали названия приложений и весь журнал
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .glassBackdropSource(backdrop)
                    // Накал ровный: он значит состояние подключения, а его показывает
                    // главный экран - здесь ему нечего сообщать
                    .liquidBackground(contentBackdrop) { 0f },
                topBar = {
                    AppTopBar(
                        title = title,
                        onBackClick = onBackClick,
                        isLoading = isLoading,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSearchClose = onSearchClose,
                        searchPlaceholder = searchPlaceholder,
                        actions = actions
                    )
                },
                content = content
            )
        }

        // Список растворяется у нижнего края, а не обрывается о него
        if (bottomFade) {
            BottomBlurScrim(
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            floatingActionButton()
        }
    }
}

/** Скругление стеклянных карточек со списками. */
val ContentCardShape = RoundedCornerShape(26.dp)

/**
 * Стеклянная карточка внутри содержимого экрана - та же, что на карточках серверов.
 *
 * Цвет берётся из палитры приложения, а не нейтральной белёсой плёнкой: с ней
 * карточки выбивались из темы, потому что все прочие поверхности окрашены
 * surfaceContainerHigh. Разложение по краю на цветные каёмки выключено - это
 * отдельный тяжёлый шейдер, а карточек на экране много.
 *
 * Слой берётся сам: карточке внутри содержимого доступен только слой фона, и
 * ошибиться тут легче, чем кажется - общий слой пишет сам экран, и рисовать его
 * внутри его же записи запрещено.
 *
 * @param refracts Брать ли слой фона под преломление. Крупным карточкам во всю
 *   ширину это ни к чему: под ними ровный градиент, размывать в нём нечего, а
 *   линза считается каждый кадр и на всю площадь - на длинных списках это первое,
 *   что съедает кадры. Край такой карточки держит блик, и этого хватает.
 */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    shape: Shape = ContentCardShape,
    refracts: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        backdrop = if (refracts) LocalContentBackdrop.current else null,
        opaqueness = 0.35f,
        surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh
            .copy(alpha = if (LocalDarkTheme.current) 0.52f else 0.58f),
        dispersion = false,
        fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        content = content
    )
}
