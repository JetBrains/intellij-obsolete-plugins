package com.intellij.bigdatatools.zeppelin.constants

object ZeppelinTimeouts {
  val DRIVER_TIMEOUT: Long = java.lang.Long.getLong("bdt.zeppelin.timeout.driver", 15 * 1000L)
  val CONNECTION_TIMEOUT: Long = java.lang.Long.getLong("bdt.zeppelin.timeout.connection", 10 * 1000L)
  val READ_TIMEOUT: Long = java.lang.Long.getLong("bdt.zeppelin.timeout.read", 10 * 1000L)
  val IDLE_TIMEOUT: Long = java.lang.Long.getLong("bdt.zeppelin.timeout.idle", 20 * 1000L)
  val GET_NOTE_TIMEOUT: Long = java.lang.Long.getLong("bdt.zeppelin.timeout.note", 30 * 1000L)
  val DOWNLOAD_DEP_TIMEOUT: Int = Integer.getInteger("bdt.zeppelin.timeout.note",  90 * 1000)
}