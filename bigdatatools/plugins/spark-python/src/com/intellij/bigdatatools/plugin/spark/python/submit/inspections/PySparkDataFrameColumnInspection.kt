package com.intellij.bigdatatools.plugin.spark.python.submit.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor

class PySparkDataFrameColumnInspection : PyInspection() {
  override fun buildVisitor(holder: ProblemsHolder,
                            isOnTheFly: Boolean,
                            session: LocalInspectionToolSession): PsiElementVisitor {
    return PySparkDataframeAnnotatorVisitor(holder, PyInspectionVisitor.getContext(session))
  }
}

