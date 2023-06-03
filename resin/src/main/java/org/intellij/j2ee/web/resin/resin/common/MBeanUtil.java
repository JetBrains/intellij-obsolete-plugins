package org.intellij.j2ee.web.resin.resin.common;

import org.jetbrains.annotations.NonNls;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class MBeanUtil {

  private MBeanUtil() {

  }

  public static ObjectName newObjectName(@NonNls String objectName) {
    try {
      return ObjectName.getInstance(objectName);
    }
    catch (MalformedObjectNameException e) {
      throw new InternalError("Never happens");
    }
  }
}
