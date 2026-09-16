package com.intellij.bigdatatools.zeppelin.models.notebook

data class ZeppelinSearchResponse(val id: String,
                                  val fileName: String = "",
                                  val query: String = "",
                                  val name: String = "",
                                  val snippet: String = "",
                                  val text: String = "",
                                  var connName: String = "",
                                  var connId: String = "",
                                  val header: String = "")