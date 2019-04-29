/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.general;

import com.intellij.openapi.util.Comparing;

public class UserDefinedOption {

  String myKey;
  String myValue;

  public UserDefinedOption(String key, String value) {
    myKey = key;
    myValue = value;
  }

  public String getKey() {
    return myKey;
  }

  public void setKey(final String key) {
    myKey = key;
  }

  public String getValue() {
    return myValue;
  }

  public void setValue(final String value) {
    myValue = value;
  }

  public boolean equals(final Object o) {
    if (!(o instanceof UserDefinedOption)) return false;
    final UserDefinedOption second = (UserDefinedOption)o;
    return Comparing.equal(myKey, second.myKey) && Comparing.equal(myValue, second.myValue);
  }

  public int hashCode() {
    return Comparing.hashcode(myKey, myValue);
  }

}
