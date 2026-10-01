package com.intellij.aidebugger.evaluation.settings

import com.intellij.ide.Region
import com.intellij.ide.RegionSettings
import java.util.Locale

fun isChinaRegion(): Boolean {
    return when (RegionSettings.getRegion()) {
        Region.CHINA -> true
        Region.NOT_SET -> Locale.CHINA.country == Locale.getDefault().country
        else -> false
    }
}