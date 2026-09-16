package com.intellij.bigdatatools.zeppelin.utils;

import scala.collection.immutable.Set;
import scala.collection.immutable.Set$;

public final class ScalaIntegrationUtil {
  public static <T> Set<T> createEmptyScalaSet() {
    return Set$.MODULE$.empty();
  }
}
