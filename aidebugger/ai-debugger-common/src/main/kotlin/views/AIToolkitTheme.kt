package com.intellij.aidebugger.common.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ui.JBColor
import org.jetbrains.jewel.ui.icon.PathIconKey

class AIToolkitThemeData(val isDark: Boolean = !JBColor.isBright()) {
    val primaryTextColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color(0xFF868A91) else Color(0xFF444444)
    val tertiaryTextColor = if (isDark) Color(0xFFB4B8BF) else Color(0xFF444444)
    val disabledTextColor = if (isDark) Color(0xFF767A8A) else Color(0xFFAAAAAA)

    val menuHeaderColor =  if (isDark) Color(0xFF6F737A) else Color(0xFF787878)

    val selectedNodeColor = if (isDark) Color(0xFF393B40) else Color(0xFFF0F0F0)
    val nodeConnectionColor = if (isDark) Color(0xFF393B40) else Color(0xFFD0D0D0)
    val nodeBorderColor = if (isDark) Color.Transparent else Color(0xFFC9CCD6)

    val panelBackgroundColor = if (isDark) Color(0xFF393B40) else Color(0xFFF7F8FA)
    val panelTitleBackgroundColor = if (isDark) Color(0xFF494B57) else Color(0xFFEBECF0)
    val panelBorderColor = if (isDark) Color(0xFF565660) else Color(0xFFC9CCD6)
    val toolbarBorderColor = if (isDark) Color(0xFF1E1F22) else Color(0xFFE0E1E7)
    val toolsNameBackgroundColor = if (isDark) Color(0xFF43454A) else Color(0xFFF0F0F0)
    val toolsTableBorderColor = if (isDark) Color(0xFF4E5157) else Color(0xFFE0E1E7)

    val separatorColor = if (isDark) Color(0xFF393B40) else Color(0xFFEBECF0)

    val greenDotColor = if (isDark) Color(0xFF436946) else Color(0xFFC3E6CB)
    val greenDotTextColor = if (isDark) Color(0xFFD4FAD7) else Color(0xFF155724)

    val buttonHoverColor = if (isDark) Color(0xFF393B40) else Color(0xFFEBECF0)
    val buttonSelectedColor = if (isDark) Color(0xFF4E5157) else Color(0xFFDFE1E5)

    val chipBackground = if (isDark) Color(0xFF2B2D30) else Color(0xFFF7F8FA)

    val jsonFormatterTheme = JsonFormatterTheme(isDark)


    val h1: TextUnit = 20.sp
    val h2: TextUnit = 18.sp
    val h3: TextUnit = 16.sp
    val regularTextFontSize: TextUnit = 13.sp
    val smallerTextFontSize: TextUnit = 12.sp

    val panelRounding = 16.dp

    val typicalSpace0 = 4.dp
    val typicalSpace1 = 8.dp
    val typicalSpace2 = 10.dp
    val typicalSpace3 = 12.dp
    val typicalSpace4 = 16.dp
    val typicalSpace5 = 32.dp
    val smallGap = 2.dp
    val itemsInterval = 17.dp

    val connectionLineSize = 2.dp

    val infoIcon = iconPath("events/info")
    val exceptionIcon = iconPath("events/exception")

    val nodeAiIcon = iconPath("events/node_ai")
    val nodeToolIcon = iconPath("events/node_tool")
    val nodeModeRawIcon = iconPath("events/node_mode_raw")
    val nodeModeNiceIcon = iconPath("events/node_mode_nice")
    val progressIcon = iconPath("widgets/progress")

    fun iconByPath(path: String) = iconPath(path, isDark)

    private fun iconPath(path: String, isDark: Boolean = this.isDark) =
        PathIconKey(if (isDark) "/icons/${path}_dark.svg" else "/icons/${path}.svg", AIToolkitThemeData::class.java)
}

data class JsonFormatterTheme(
    val isDark: Boolean = !JBColor.isBright(),
    val keyColor: Color = if (isDark) Color(0xFF80CBC4) else Color(0xFF0277BD),        // Dark: Teal, Light: Deep Blue
    val stringColor: Color = if (isDark) Color(0xFFCE9178) else Color(0xFF388E3C),     // Dark: Orange, Light: Green
    val numberColor: Color = if (isDark) Color(0xFF4FC1FF) else Color(0xFF1976D2),     // Dark: Light Blue, Light: Blue
    val boolColor: Color = if (isDark) Color(0xFFC586C0) else Color(0xFF7B1FA2),       // Dark: Purple, Light: Deep Purple
    val nullColor: Color = if (isDark) Color(0xFF9AA0A6) else Color(0xFF757575),       // Dark: Gray, Light: Dark Gray
    val punctColor: Color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF424242),      // Dark: Light Gray, Light: Dark Gray
    val textColor: Color = if (isDark) Color.White else Color.Black                   // Dark: White, Light: Black
)

val AIToolkitThemeProvider: ProvidableCompositionLocal<AIToolkitThemeData> = staticCompositionLocalOf {
    error("CompositionLocal AIToolkitThemeProvider not provided")
}

val AIToolkitTheme: AIToolkitThemeData
    @Composable get() = AIToolkitThemeProvider.current