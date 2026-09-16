def calculate_variables():
    import types
    import json
    import traceback
    from pyspark.sql import DataFrame
    from pyspark.sql.types import StructType

    import time

    def current_milli_time():
        return round(time.time() * 1000)

    # Main params
    origin_deep = %s
    string_size = %s
    collection_size = %s
    timeout = %s
    white_list_names = %s

    black_list_names = ["z", "intp", "gateway", "jconf", "jsc", "conf", "sqlc", "sqlContext", "jarlist"]
    start_all_time = current_milli_time()
    ignored_types = [types.FunctionType, types.ModuleType, types.BuiltinFunctionType, types.MethodType, type]
    errors = []

    def check_timeout() -> bool:
        return current_milli_time() - start_all_time > timeout

    def add_timeout_error():
        errors.append(f"Timeout error. Collect Python variables exceed {timeout / 1000} seconds.")

    class ResNames:
        REF = "ref"
        VALUE = "value"
        KEY = "key"
        IS_PRIMITIVE = "isPrimitive"
        TYPE = "type"
        TIME = "time"
        LENGTH = "length"
        LAZY = "lazy"

    def process_variable_type(v):
        var_type = str(type(v))
        var_type = var_type[:-2]
        if var_type[0:8] == "<class '":
            var_type = var_type[8:]
        elif var_type[0:7] == "<type '":
            var_type = var_type[7:]
        return var_type

    def process_variable(v, deep: int):
        var_type = ""
        try:
            if isinstance(v, str):
                if len(v)>string_size:
                  v = v[:string_size]+"..."
                return v
            if v is None or isinstance(v, str) or isinstance(v, int) or isinstance(v, bool):
                return v

            var_type = process_variable_type(v)
            mapa = {
                ResNames.TYPE: var_type,
            }

            if check_timeout():
                add_timeout_error()
                return mapa

            from pyspark.sql.types import StructField
            from pyspark.rdd import RDD
            from pyspark.context import SparkContext
            from pyspark.sql.session import SparkSession
            if deep == 0:
                v = str(v)
                if len(v)>string_size:
                                 v = v[:string_size]+"..."
                mapa[ResNames.VALUE] = v
            elif isinstance(v, list) or isinstance(v, tuple):
                mapa[ResNames.LENGTH] = len(v)
                mapa[ResNames.VALUE] = [process_variable(it, deep=deep - 1) for it in v[:collection_size]]
            elif isinstance(v, set):
                mapa[ResNames.LENGTH] = len(v)
                set_vars = []
                count = 0
                for it in v:
                    if count == collection_size:
                        break
                    set_vars.append(it)
                    count += 1
                mapa[ResNames.VALUE] = [process_variable(it, deep=deep - 1) for it in set_vars]
            elif isinstance(v, dict):
                mapa[ResNames.LENGTH] = len(v)
                dict_vars = []
                count = 0
                for it in v.keys():
                    if count == collection_size:
                        break
                    dict_vars.append(it)
                    count += 1
                mapa[ResNames.KEY] = [process_variable(it, deep=0) for it in dict_vars]
                dict_vars = []
                count = 0
                for it in v.values():
                    if count == collection_size:
                        break
                    dict_vars.append(it)
                    count += 1
                mapa[ResNames.VALUE] = [process_variable(it, deep=deep - 1) for it in dict_vars[:collection_size]]
            elif isinstance(v, Exception):
                mapa[ResNames.VALUE] = str(v) + str(traceback.format_exc())
            elif isinstance(v, StructType):
                mapa[ResNames.VALUE] = [process_variable(f, deep=1) for f in v.fields]
            elif isinstance(v, StructField):
                mapa[ResNames.VALUE] = {
                    "name": v.name,
                    "nullable": v.nullable,
                    "dataType": v.dataType.typeName(),
                }
            elif isinstance(v, DataFrame):
                mapa[ResNames.VALUE] = {
                    "schema()": process_variable(v.schema, deep=2),
                    "getStorageLevel()": str(v.storageLevel)
                }
            elif isinstance(v, RDD):
                mapa[ResNames.VALUE] = {
                    "getNumPartitions()": v.getNumPartitions(),
                    "name": v.name(),
                    "id": v.id(),
                    "partitioner": str(v.partitioner),
                    "getStorageLevel()": str(v.getStorageLevel()),
                }
            elif isinstance(v, SparkContext):
                mapa[ResNames.VALUE] = {
                    "sparkUser": v.sparkUser(),
                    "startTime": v.startTime,
                    "applicationId()": v.applicationId,
                    "appName()": str(v.appName),
                }
            elif isinstance(v, SparkSession):
                mapa[ResNames.VALUE] = {
                    "version()": v.version,
                    "sparkContext": process_variable(v.sparkContext, deep=1),
                }
            elif hasattr(v, "__dict__"):
                mapa[ResNames.VALUE] = {field_name: process_variable(field_value, deep=deep - 1) for
                                        field_name, field_value
                                        in v.__dict__.items()}
            else:
                v = str(v)
                if len(v)>string_size:
                                 v = v[:string_size]+"..."
                mapa[ResNames.VALUE] = v
            return mapa
        except Exception as t:
            return {
                ResNames.TYPE: var_type,
                ResNames.VALUE: (str(err) + str(traceback.format_exc()))
            }

    variables = dict()
    items = globals().items()
    for k, v in items:
        if k.startswith("_") or k in black_list_names or (white_list_names is not None and k not in white_list_names):
            continue
        is_type_ignored = False
        for ignType in ignored_types:
            if isinstance(v, ignType):
                is_type_ignored = True
                break
        if is_type_ignored:
            continue
        if check_timeout():
            add_timeout_error()
            break
        variables[k] = process_variable(v, origin_deep)
    json_value = json.dumps({
        "variables": variables,
        "errors": errors
    })
    print("---ztools-scala---", end="\n")
    print(json_value, end="\n")
    print("---ztools-scala---", end="\n")
try:
    calculate_variables()
except Exception as err:
    import json
    import traceback


    def collect_errors():
        return json.dumps({
            "variables": {},
            "errors": [str(err) + str(traceback.format_exc())]
        })


    print("---ztools-scala---", end="\n")
    print(collect_errors(), end="\n")
    print("---ztools-scala---", end="\n")