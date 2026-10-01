package com.intellij.aidebugger.common.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.AiDebuggerIcons
import com.intellij.aidebugger.common.views.components.HtmlText
import com.intellij.ide.BrowserUtil
import com.intellij.util.ui.StartupUiUtil
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

@OptIn(ExperimentalResourceApi::class)
@Suppress("UnstableApiUsage")
@Composable
fun LicenseRequirementsNotMetView(modifier: Modifier = Modifier) = commonMessageView(modifier, 16.dp) {
    Text(
        fontSize = AIToolkitTheme.h3,
        text = AiDebuggerBundle.message("aitoolkit.debugger.licenseRequirementsNotMet.title")
    )

    HtmlText(AiDebuggerBundle.message("aitoolkit.debugger.licenseRequirementsNotMet.ourFeatures"))

    var screenshotPath: String
    var screenshotModifier: Modifier
    if (StartupUiUtil.isDarkTheme) {
        screenshotPath = "images/aiDebuggerScreenshot_dark.png"
        screenshotModifier = Modifier.border(BorderStroke(1.dp, Color.DarkGray))
    } else {
        screenshotPath = "images/aiDebuggerScreenshot.png"
        screenshotModifier = Modifier.border(BorderStroke(1.dp, Color.LightGray))
    }

    val screenshotBytes = checkNotNull(AiDebuggerIcons::class.java.classLoader.getResourceAsStream(screenshotPath)) {
        "Could not load resource $screenshotPath: it does not exist or can't be read."
    }.readAllBytes()
    val painter = BitmapPainter(screenshotBytes.decodeToImageBitmap())
    Image(painter = painter, contentDescription = null, modifier = screenshotModifier)

    DefaultButton(
        onClick = {
            BrowserUtil.browse(AiDebuggerBundle.message("aitoolkit.debugger.licenseRequirementsNotMet.upgradeLink"))
            AiDebuggerCollector.reportBuyPycharmProClicked()
                  },
        content = {
            Text(text = AiDebuggerBundle.message("aitoolkit.debugger.licenseRequirementsNotMet.upgradeButtonText"))
        }
    )
}