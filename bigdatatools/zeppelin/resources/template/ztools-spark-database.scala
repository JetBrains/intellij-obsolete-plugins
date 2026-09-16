{
    var sqlTableShows: Array[String] = %s
    val additionalTables = Array[Tuple2[String, String]](%s)
    val timeout = %s
    val collectOnlyTempTables = %s
    val appendOutput = %s

    case class ZtoolsColumn(name: String,
                            columnType: String,
                            description: String)

    case class ZtoolsTable(name: String,
                           databaseName: String,
                           var columns: Array[ZtoolsColumn],
                           var error: String = null)

    case class ZtoolsSqlProfile(request: String, time: Long)

    case class ZtoolsSqlInfo(tables: Array[ZtoolsTable],
                             errors: Array[String],
                             profiling: Array[ZtoolsSqlProfile],
                             appendOutput: Boolean = appendOutput)


    //TO KNOW:
    //We collect info by spark.sql not spark.catalog because there some errors with Glue, database does not read
    //Additionally we cannot use column name because it can be different "namespace" in EMR and "database" in vanilla spark
    def calcZtoolsSqlSchemas(): String = {
        import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
        import com.fasterxml.jackson.annotation.PropertyAccessor
        import com.fasterxml.jackson.databind.ObjectMapper
        import org.apache.commons.lang.exception.ExceptionUtils
        import org.apache.spark.sql.Row

        import scala.collection.mutable.ArrayBuffer

        val startTime = System.currentTimeMillis()
        val errors = ArrayBuffer[String]()

        def convertThrowable(msg: String, t: Throwable): String = msg + "\n" +
                ExceptionUtils.getRootCauseMessage(t) + "\n" +
                ExceptionUtils.getStackTrace(t)

        def escapeSql(string: String) = "`" + string.replace("`", "``") + "`"




        var tables = ArrayBuffer[ZtoolsTable]()
        var profilingResult = ArrayBuffer[ZtoolsSqlProfile]()

        def performSql(sqlRequest: String): Tuple2[Array[_ <: Row], String] = {
            if (System.currentTimeMillis() - startTime > timeout) {
                val error = f"Timeout $timeout exceed. Sql request '$sqlRequest' ignored."
                errors.append(error)
                return (Array.empty, error)
            }
            val startTransactionTime = System.currentTimeMillis()
            try {
                val rows = spark.sql(sqlRequest).collect()
                (rows, null)
            } catch {
                case t: Throwable =>
                    errors.append(convertThrowable(sqlRequest, t))
                    (Array.empty, ExceptionUtils.getMessage(t))
            } finally {
                profilingResult += ZtoolsSqlProfile(sqlRequest, System.currentTimeMillis() - startTransactionTime)
            }
        }

        if (sqlTableShows!=null && sqlTableShows.isEmpty) {
            val sqlRequest = "show databases"
            val databases = performSql(sqlRequest)._1.map(_.getAs[String](0))
            sqlTableShows = databases.map(db => f"SHOW TABLES in $db")
        }

        if (sqlTableShows==null) {
            sqlTableShows = Array.empty
        }

        sqlTableShows.foreach(sqlRequest => {
            try {
                var listTables = performSql(sqlRequest)._1
                if (collectOnlyTempTables)
                    listTables = listTables.filter(_.getAs[Boolean](2) == true)

                listTables.map(row => ZtoolsTable(
                    databaseName = row.getAs[String](0),
                    name = row.getAs[String](1),
                    columns = Array.empty[ZtoolsColumn])).foreach(t => tables.append(t))
            } catch {
                case t: Throwable =>
                    errors.append(convertThrowable(s"Error transform output of  $sqlRequest", t))
                    ArrayBuffer.empty[ZtoolsTable]
            }
        })

        val tableSet = (additionalTables.map(it => ZtoolsTable(it._2, it._1, Array.empty)) ++ tables).distinct

        def processTable(table: ZtoolsTable): Unit = {
            val columns = try {
                val tableSqlName = if (table.databaseName == null || table.databaseName.isEmpty)
                    escapeSql(table.name)
                else
                    escapeSql(table.databaseName) + "." + escapeSql(table.name)

                //https://spark.apache.org/docs/3.0.0-preview/sql-ref-syntax-aux-describe-table.html
                val sqlResult = performSql(s"DESCRIBE TABLE $tableSqlName")

                val columnRows = sqlResult._1
                table.error = sqlResult._2

                //Ignore partition section
                columnRows.takeWhile(row => !Option(row.getAs[String](0)).getOrElse("").startsWith("# "))
                        .map(row => ZtoolsColumn(row.getAs[String](0), row.getAs[String](1), row.getAs[String](2)))
            } catch {
                case t: Throwable => convertThrowable(s"Error list columns for ${table.name}", t)
                    table.error = ExceptionUtils.getRootCauseMessage(t)
                    errors.append(convertThrowable(s"Error list columns for ${table.name}", t))
                    return
            }
            table.columns = columns
        }

        tableSet.foreach(table => {
            processTable(table)
        })

        val res = ZtoolsSqlInfo(tableSet.toArray, errors.toArray, profilingResult.toArray)
        val objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY).writerWithDefaultPrettyPrinter()
        objectMapper.writeValueAsString(res)
    }

    def ztoolsPrintResult(): Unit = {
        val ztoolsSqlResult = calcZtoolsSqlSchemas()
        println("---ztools-sql---")
        println(ztoolsSqlResult)
        println("---ztools-sql---")
    }

    ztoolsPrintResult()
}