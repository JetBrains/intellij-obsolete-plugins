package com.intellij.dbt

import com.intellij.database.model.DasObject
import com.intellij.database.model.DasTypeAwareObject
import com.intellij.database.model.ObjectKind
import com.intellij.database.symbols.DasSymbol
import com.intellij.database.types.DasType
import com.intellij.database.types.DasTypeSystemBase
import com.intellij.database.util.DbImplUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.sql.dialects.ReservedEntity
import com.intellij.sql.psi.SqlCompositeElementTypes.SQL_TABLE_REFERENCE
import com.intellij.sql.psi.SqlLanguage
import com.intellij.sql.psi.SqlQueryExpression
import com.intellij.sql.psi.SqlReference
import com.intellij.sql.psi.SqlScopeProcessor
import com.intellij.sql.psi.impl.SqlResolveExtension
import com.intellij.sql.psi.impl.SqlTableTypeBase
import com.intellij.sql.psi.impl.SqlTypeFactory
import com.intellij.util.containers.JBIterable

class DbtSqlExtension : SqlResolveExtension {
  override fun process(reference: SqlReference, processor: SqlScopeProcessor): Boolean {
    val element = reference.element
    if (element.elementType != SQL_TABLE_REFERENCE) {
      return true
    }
    val sqlQueryExpression = PsiTreeUtil.getParentOfType(element, SqlQueryExpression::class.java) ?: return true
    val fromExpression = sqlQueryExpression.tableExpression?.fromClause?.fromExpression ?: return true

    val jinjaCall = DbtUtils.getJinjaCall(fromExpression) ?: return true
    if (!DbtUtils.isRefCall(jinjaCall)) {
      return true
    }


    val referencedName = DbtUtils.getReferencedName(jinjaCall) ?: return true
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return true
    val modelPsiFile = DbtDirectories.findModel(referencedName, module)
    if (modelPsiFile != null) {
      val sqlDialect = modelPsiFile.viewProvider.languages.firstOrNull { it.isKindOf(SqlLanguage.INSTANCE) } ?: return true
      val sqlFile = modelPsiFile.viewProvider.getPsi(sqlDialect) ?: return true
      val targetSqlQueryExpression = DbtUtils.findLastSelectQuery(sqlFile) ?: return true

      return processor.executeTarget(DbtModelDasSymbol(targetSqlQueryExpression, element.text, null), null, null, ResolveState.initial())
    }

    val seedPsiFile = DbtDirectories.findSeedFile(referencedName, module) ?: return true
    val fakeCsvTable = DbtSeedFile(seedPsiFile)
    val dasType = DbtCsvType(seedPsiFile)
    val dasObject = ReservedEntity.Typed(DbImplUtil.getDbms(element), element.text, ObjectKind.TABLE, dasType)
    val symbol = DbtModelDasSymbol(fakeCsvTable, element.text, dasObject)
    return processor.executeTarget(symbol, dasType, null, ResolveState.initial())
  }

  class DbtModelDasSymbol(private val element: PsiElement, private val name: String, private val dasObject: DasObject?) : UserDataHolderBase(), DasSymbol {
    override fun getName(): String = name
    override fun getKind(): ObjectKind = ObjectKind.TABLE
    override fun isQuoted() = false
    override fun getDbms() = DbImplUtil.getDbms(element)
    override fun isValid() = true
    override fun getDasObject() = dasObject
    override fun getPsiDeclarations() = JBIterable.of(element)
    override fun getNavigationElement() = element
    override fun getContextElement() = element
  }

  class DbtSeedFile(private val originalFile: PsiFile) : FakePsiElement(), DasTypeAwareObject {
    override fun getParent() = originalFile

    override fun getDasType(): DasType {
      return DbtCsvType(originalFile)
    }
  }

  class DbtCsvType(file: PsiFile) : SqlTableTypeBase() {
    private val columnNames = mutableListOf<String>()
    private val columnOffsets = mutableListOf<Int>()
    private val columns = mutableListOf<PsiElement>()

    init {
      var lastSeparatorOffset = -1
      for (i in 0..<file.textLength) {
        val c = file.text[i]
        if (c == ',' || c == '\n') {
          columnNames.add(file.text.substring(lastSeparatorOffset + 1, i).trim())
          columnOffsets.add(lastSeparatorOffset + 1)
          lastSeparatorOffset =  i
        }
        if (c == '\n') {
          break
        }
      }

      for (i in 0..<columnNames.size) {
        columns.add(CsvColumnElement(getColumnName(i), file, columnOffsets[i]))
      }
    }

    override fun toDataType() = SqlTypeFactory.createTableDataType("")
    override fun getColumnCount() = columns.count()
    override fun getColumnName(i: Int) = columnNames[i]
    override fun getColumnDasType(i: Int) = DasTypeSystemBase.UNKNOWN
    override fun getMethods() = mutableListOf<DasObject>()
    override fun isColumnQuoted(i: Int) = false
    override fun getColumnElement(i: Int) = columns[i]
    override fun getSourceColumnElement(i: Int) = null
    override fun getColumnQualifier(i: Int) = null
  }

  class CsvColumnElement(private val name: String, private val parent: PsiFile, private val offset: Int) : FakePsiElement(), DasSymbol {
    override fun getParent() = parent
    override fun getName(): String = name
    override fun getKind(): ObjectKind = ObjectKind.COLUMN
    override fun isQuoted() = false
    override fun getDbms() = DbImplUtil.getDbms(this)
    override fun isValid() = true
    override fun getDasObject(): DasObject? = null
    override fun getPsiDeclarations() = JBIterable.of(this)
    override fun getNavigationElement() = this
    override fun getContextElement() = this
    override fun getTextOffset() = offset
    override fun getText() = name
  }
}