package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.type

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.COLUMN_CLASS_NAME
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts.DATAFRAME_CLASS_NAME
import com.intellij.openapi.util.Ref
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyCallSiteExpression
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkTypeProvider : PyTypeProviderBase() {
  override fun getReferenceExpressionType(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
    val parent = referenceExpression.qualifier as? PyTypedElement ?: return null
    if (!isType(parent, context, DATAFRAME_CLASS_NAME))
      return null

    if (referenceExpression.nextSibling is PyArgumentList)
      return null
    if (referenceExpression.getReference(PyResolveContext.defaultContext(context)).resolve() != null)
      return null

    return createClassType(referenceExpression, COLUMN_CLASS_NAME)
  }


  private fun isType(element: PyElement?, context: TypeEvalContext, classTypeFqn: String): Boolean {
    if (element !is PyTypedElement)
      return false

    val type = (context.getType(element) as? PyClassType)?.classQName ?: return false
    return type == classTypeFqn
  }

  override fun getCallType(function: PyFunction, callSite: PyCallSiteExpression, context: TypeEvalContext): Ref<PyType>? {
    if (callSite is PyCallExpression) {
      val callee = callSite.callee as? PyReferenceExpression ?: return null
      if (callee.referencedName != "where")
        return null
      val qualifier = callee.qualifier ?: return null
      if (!isType(qualifier, context, DATAFRAME_CLASS_NAME))
        return null
      val type = createClassType(callee, DATAFRAME_CLASS_NAME)
      return Ref.create(type)
    }
    else {
      val expression = callSite as? PyBinaryExpression ?: return null
      val type = getBinaryExpressionType(expression, context) ?: return null
      return Ref.create(type)
    }
  }

  private fun getBinaryExpressionType(expression: PyBinaryExpression, context: TypeEvalContext): PyType? {
    return if (isType(expression.leftExpression, context, COLUMN_CLASS_NAME) ||
               isType(expression.rightExpression, context, COLUMN_CLASS_NAME))
      createClassType(expression, COLUMN_CLASS_NAME)
    else
      null
  }

  @Suppress("SameParameterValue")
  private fun createClassType(referenceExpression: PyExpression,
                              className: String): PyType? {
    val facade = PyPsiFacade.getInstance(referenceExpression.project)
    val pyClass = facade.createClassByQName(className, referenceExpression)

    return if (pyClass != null)
      facade.createClassType(pyClass, false)
    else
      null
  }
}