package com.intellij.bigdatatools.zeppelin.utils

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch
import java.util.LinkedList

object PatchUtils {
  fun applyPatch(text: String, patch: String): String {
    val diffMatchPatch = DiffMatchPatch()
    val patchObj = diffMatchPatch.patchFromText(patch).toList()
    val linkedPath = LinkedList(patchObj)
    return diffMatchPatch.patchApply(linkedPath, text)[0].toString()
  }

  fun getPatch(oldText: String, newText: String): String {
    val diffMatchPatch = DiffMatchPatch()
    val diffs = diffMatchPatch.patchMake(oldText, newText)
    return diffMatchPatch.patchToText(diffs)
  }
}