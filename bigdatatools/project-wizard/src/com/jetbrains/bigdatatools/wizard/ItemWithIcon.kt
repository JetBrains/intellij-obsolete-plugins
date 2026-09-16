package com.jetbrains.bigdatatools.wizard

import javax.swing.Icon

interface ItemWithIcon {
  val presentableName: String
  val icon: Icon?
}