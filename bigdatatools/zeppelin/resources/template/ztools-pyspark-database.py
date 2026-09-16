import traceback
import json
import time


# TO KNOW:
# We collect info by spark.sql not spark.catalog because there some errors with Glue, database does not read
# Additionally we cannot use column name because it can be different "namespace" in EMR and "database" in vanilla spark
def calc_ztools_sql_schemas():
    def current_milli_time():
        return round(time.time() * 1000)

    def escape_sql(string: str):
        return "`" + string.replace("`", "``") + "`"

    sql_table_shows = %s
    additional_tables = [%s]
    timeout = %s
    collect_only_temp_tables = %s
    append_output = %s

    tables: list = []
    errors = []
    profiling_result = []
    start_time = current_milli_time()

    def perform_sql(sql_request):
        cur_time = current_milli_time()
        if cur_time - start_time > timeout:
            error = f"Timeout {timeout} exceed. Sql request '{sql_request}' ignored."
            errors.append(error)
            return [], error
        try:
            rows = spark.sql(sql_request).collect()
            return rows, None
        except Exception:
            error = traceback.format_exc()
            errors.append(f"Error perform {sql_request}. " + error)
            return [], error
        finally:
            profiling_result.append({"request": sql_request, "time": current_milli_time() - cur_time})

    if sql_table_shows is not None:
      if len(sql_table_shows) == 0:
          databases = [x[0] for x in perform_sql("show databases")[0]]
          sql_table_shows = [f"SHOW TABLES in {db}" for db in databases]

      for request in sql_table_shows:
          try:
              list_tables = perform_sql(request)[0]
              if collect_only_temp_tables:
                  list_tables = [t for t in list_tables if t[2] is True]
              tables += [{"databaseName": row[0],
                          "name": row[1],
                          "columns": []} for row in list_tables]
          except Exception:
              errors.append(f"Error transform output of {request}" + traceback.format_exc())

    all_tables = [{"databaseName": t[0],
                   "name": t[1],
                   "columns": []} for t in additional_tables]
    all_tables.extend(tables)
    tables = all_tables

    for table in all_tables:
        table_database: str = table['databaseName']
        table_name = table['name']
        if table_database is None or table_database == "default" or not table_database:
            sql_name = escape_sql(table_name)
        else:
            sql_name = escape_sql(table_database) + "." + escape_sql(table_name)

        columns = []
        try:
            table_describe_res = perform_sql(f"DESCRIBE TABLE {sql_name}")
            rows = table_describe_res[0]
            table['error'] = table_describe_res[1]

            for row in rows:
                col_name: str = row[0]
                if not col_name:
                    continue
                if col_name.startswith("# "):
                    break
                columns.append({
                    "name": col_name,
                    "columnType": row[1],
                    "description": row[2]
                })
        except Exception:
            errors.append(f"Error list columns for {sql_name}\n" + traceback.format_exc())
        table["columns"] = columns

    def collect_info_to_json() -> str:
        info = {"profiling": profiling_result, "tables": tables, "errors": errors, "appendOutput": append_output}
        return json.dumps(info, sort_keys=True, indent=2)

    json_str = collect_info_to_json()
    print("---ztools-sql---", sep="\n")
    print(json_str, sep="\n")
    print("---ztools-sql---", sep="\n")


calc_ztools_sql_schemas()
