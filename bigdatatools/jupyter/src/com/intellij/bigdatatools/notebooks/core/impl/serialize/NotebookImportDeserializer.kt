/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.bigdatatools.notebooks.core.impl.serialize

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * importNote date format deserializer
 */
class NotebookImportDeserializer : JsonDeserializer<Date> {
  @Throws(JsonParseException::class)
  override fun deserialize(jsonElement: JsonElement, typeOF: Type,
                           context: JsonDeserializationContext): Date {
    for (format in DATE_FORMATS) {
      try {
        return SimpleDateFormat(format, Locale.US).parse(jsonElement.asString)
      }
      catch (e: ParseException) {
      }
    }
    throw JsonParseException("Unparsable date: \"" + jsonElement.asString
                             + "\". Supported formats: " + DATE_FORMATS.contentToString())
  }

  companion object {
    val DATE_FORMATS = arrayOf(
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "MMM d, yyyy h:mm:ss a",
      "MMM dd, yyyy HH:mm:ss",
      "MMM dd, yyyy, HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss.SSS")
  }
}