package com.jetbrains.spark.monitoring.connection

import com.intellij.bigdatatools.coreUi.connection.exception.BdtWrongFormatResponseException
import com.jetbrains.spark.monitoring.data.SqlInfo
import com.jetbrains.spark.monitoring.data.SqlInfoStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

object SparkHtmlParser {
  fun parseSql(text: String) = try {
    val document = Jsoup.parse(text)
    val completedSqlInfos = parseCompletedQueries(document)
    val failedSqlInfos = parseFailedQueries(document)
    val runningSqlInfos = parseRunningQueries(document)

    completedSqlInfos + failedSqlInfos + runningSqlInfos
  }
  catch (e: Exception) {
    throw SparkWrongFormatSqlHtmlException()
  }

  fun parseDAG(text: String): Pair<List<String>, List<String>> {
    val graphsWithExternalEdges = try {
      val document = Jsoup.parse(text)
      val graphsTag = document.getElementsByClass("dot-file")
      if (graphsTag.isEmpty())
        throw Exception("DAG not found")

      val graphs = graphsTag.eachText()
      val externalEdges = document.getElementsByClass("incoming-edge").eachText()

      Pair(graphs, externalEdges)
    }
    catch (e: Exception) {
      error("Cannot parse DAG for Job HTML page:\n${e.message}")
    }
    return graphsWithExternalEdges
  }

  class SparkWrongFormatSqlHtmlException : BdtWrongFormatResponseException() {
    override val shortDescription: String = "Cannot parse Sql HTML page"
  }

  private fun toStringList(data: String) = data.replace("[", "").replace("]", ",")
    .split(",")
    .filterNot { it.isBlank() }

  private fun parseCompletedQueries(document: Document): List<SqlInfo> {
    val rows = getRowsFromTable(document, "completed-execution-table")
               ?: getRowsFromTable(document, "completed-table")
               ?: return emptyList()
    return rows.mapNotNull { row ->
      val fields = row?.children() ?: return@mapNotNull null
      val (id, _, submitted, duration, jobs) = fields.map { it.text() }
      val (descriptionLink, descriptionShort, descriptionFull) = parseDescription(fields[1])
      SqlInfo(status = SqlInfoStatus.COMPLETED,
              id = id.toInt(),
              descriptionLink = descriptionLink,
              descriptionShort = descriptionShort,
              descriptionFull = descriptionFull,
              submitted = submitted,
              duration = duration,
              succeededJobs = toStringList(jobs))
    }
  }

  private fun parseRunningQueries(document: Document): List<SqlInfo> {
    val rows = getRowsFromTable(document, "running-execution-table")
               ?: getRowsFromTable(document, "active-table")
               ?: return emptyList()
    return rows.mapNotNull { row ->
      val fields = row?.children() ?: return@mapNotNull null
      val textFields = fields.map { it.text() }
      val id = textFields[0]
      val (descriptionLink, descriptionShort, descriptionFull) = parseDescription(fields[1])
      val submitted = textFields[2]
      val duration = textFields[3]
      val runningJobs = textFields[4]
      val succeededJobs = textFields[5]
      val failedJobs = textFields[6]
      SqlInfo(status = SqlInfoStatus.RUNNING,
              id = id.toInt(),
              descriptionLink = descriptionLink,
              descriptionShort = descriptionShort,
              descriptionFull = descriptionFull,
              submitted = submitted,
              duration = duration,
              runningJobs = toStringList(runningJobs),
              succeededJobs = toStringList(succeededJobs),
              failedJobs = toStringList(failedJobs))
    }
  }

  private fun parseFailedQueries(document: Document): List<SqlInfo> {
    val rows = getRowsFromTable(document, "failed-execution-table") ?: return emptyList()
    return rows.mapNotNull { row ->
      val fields = row?.children() ?: return@mapNotNull null
      val textFields = fields.map { it.text() }
      val id = textFields[0]
      val (descriptionLink, descriptionShort, descriptionFull) = parseDescription(fields[1])
      val submitted = textFields[2]
      val duration = textFields[3]
      val succeededJobs = textFields[4]
      val failedJobs = textFields[5]
      SqlInfo(status = SqlInfoStatus.FAILED,
              id = id.toInt(),
              descriptionLink = descriptionLink,
              descriptionShort = descriptionShort,
              descriptionFull = descriptionFull,
              submitted = submitted,
              duration = duration,
              succeededJobs = toStringList(succeededJobs),
              failedJobs = toStringList(failedJobs))
    }
  }

  private fun getRowsFromTable(document: Document, tableId: String): Elements? {
    val completed = document.body().getElementById(tableId)
    val body = completed?.getElementsByTag("tbody")?.firstOrNull()
    return body?.children()
  }

  private fun parseDescription(element: Element?): List<String> {
    element ?: return listOf("", "", "")
    val (href, shortDescription) = element.getElementsByTag("a")?.let {
      listOf(it.attr("href"), it.text())
    } ?: listOf("", "")
    val longDescription = element.getElementsByTag("pre")?.text() ?: ""
    return listOf(href, shortDescription, longDescription)
  }

  fun parseIsHistory(text: String): Boolean {
    return text.contains("History Server")
  }
}