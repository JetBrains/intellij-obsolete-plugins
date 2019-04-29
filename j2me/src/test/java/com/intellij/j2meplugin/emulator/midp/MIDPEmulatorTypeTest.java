/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.emulator.midp;

import junit.framework.TestCase;

import java.util.Properties;

/**
 * @author Mike Jennings
 */
public class MIDPEmulatorTypeTest extends TestCase {
  // the following is captured from \WTK2.5.2.01\bin\emulator -version 
  private static final String WTK2_5_2_01_OUTPUT = "C:\\WTK2.5.2_01\\bin\\emulator -version\n" +
      "Sun Java(TM) Wireless Toolkit 2.5.2_01 for CLDC\n" +
      "Profile: MIDP-2.1\n" +
      "Configuration: CLDC-1.1\n" +
      "Optional: J2ME-WS-1.0,J2ME-XMLRPC-1.0,JSR179-1.0.1,JSR180-1.0.1,JSR184-1.1,JSR211-1.0,JSR226-1.0,JSR229-1.1.0,JSR234-1.0,JSR238-1.0,JSR239-1.0,JSR75-1.0,JSR82-1.1,MMAPI-1.1,SATSA-APDU-1.0,SATSA-CRYPTO-1.0,SATSA-JCRMI-1.0,SATSA-PKI-1.0,WMA-1.1,WMA-2.0"; 
  private static final String EXPECTED_WTK2_5_2_01_SDK_NAME = "Sun Java(TM) Wireless Toolkit 2.5.2_01 for CLDC";

  // the following is captured from \Java_ME_platform_SDK_3.0\bin\emulator -version
  private static final String JAVA_ME_PLATFORM_SDK_3_0_OUTPUT = "C:\\Java_ME_platform_SDK_3.0\\bin\\emulator -version\n" +
      "Java(TM) Platform Micro Edition SDK 3.0\n" +
      "Profile: FP-1.0,FP-1.1,MIDP-1.0,MIDP-2.0,MIDP-2.1,PBP-1.0,PBP-1.1,PP-1.0,PP-1.1\n" +
      "Configuration: CDC-1.0,CDC-1.1,CLDC-1.0,CLDC-1.1\n" +
      "Optional: AGUI-1.0,J2ME-WS-1.0,J2ME-XMLBASE-1.0,J2ME-XMLRPC-1.0,JSR179-1.0,JSR180-1.0.1,JSR180-1.1.0,JSR184-1.1,JSR211-1.0,JSR226-1.0,JSR229-1.1,JSR234-1.0,JSR238-1.0,JSR239-1.0,JSR256-1.0,JSR256-1.1,JSR280-1.0,JSR75-1.0,JSR82-1.1,MMAPI-1.1,SATSA-1.0,SATSA-APDU-1.0,SATSA-CRYPTO-1.0,SATSA-JCRMI-1.0,SATSA-PKI-1.0,SECOP-1.0,WMA-1.1,WMA-2.0";

  private static final String EXPECTED_JAVA_ME_PLATFORM_SDK_3_0_SDK_NAME = "Java(TM) Platform Micro Edition SDK 3.0"; 

  public void testConvertVersionOutputToProperties() {
    Properties wtkprops =  MIDPEmulatorType.convertVersionOutputToProperties(WTK2_5_2_01_OUTPUT);
    String wtkSdkName = wtkprops.getProperty(MIDPEmulatorType.SDK_NAME);
    assertEquals(EXPECTED_WTK2_5_2_01_SDK_NAME, wtkSdkName);

    Properties sdk3props =  MIDPEmulatorType.convertVersionOutputToProperties(JAVA_ME_PLATFORM_SDK_3_0_OUTPUT);
    String sdk3name = sdk3props.getProperty(MIDPEmulatorType.SDK_NAME);
    assertEquals(EXPECTED_JAVA_ME_PLATFORM_SDK_3_0_SDK_NAME, sdk3name);

  }
}
