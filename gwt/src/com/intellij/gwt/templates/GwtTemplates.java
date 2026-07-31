/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.templates;

import org.jetbrains.annotations.NonNls;

public final class GwtTemplates {
  public static final @NonNls String GWT_ENTRY_POINT_JAVA = "GwtEntryPoint.java";
  public static final @NonNls String GWT_MODULE_CSS = "GwtAppCss.css";
  public static final @NonNls String GWT_MODULE_1_4_GWT_XML = "GwtApp_1_4.gwt.xml";
  public static final @NonNls String GWT_MODULE_1_5_GWT_XML = "GwtApp_1_5.gwt.xml";
  public static final @NonNls String GWT_MODULE_1_6_GWT_XML = "GwtApp_1_6.gwt.xml";
  public static final @NonNls String GWT_MODULE_HTML = "GwtAppHtml.html";
  public static final @NonNls String GWT_MODULE_HTML_1_4 = "GwtAppHtml_1_4.html";
  public static final @NonNls String GWT_MODULE_HTML_1_6 = "GwtAppHtml_1_6.html";
  public static final @NonNls String GWT_SERVICE_JAVA = "GwtAppService.java";
  public static final @NonNls String GWT_SERVICE_JAVA_1_0 = "GwtAppService_1_0.java";
  public static final @NonNls String GWT_SERVICE_JAVA_1_5 = "GwtAppService_1_5.java";
  public static final @NonNls String GWT_SERVICE_ASYNC_JAVA = "GwtAppServiceAsync.java";
  public static final @NonNls String GWT_SERVICE_IMPL_JAVA = "GwtAppServiceImpl.java";
  public static final @NonNls String GWT_SERIAL_CLASS_JAVA = "GwtSerialClass.java";
  public static final @NonNls String GWT_TEST_CASE_JAVA = "GwtTestCase.java";
  public static final @NonNls String UI_BINDER_JAVA = "GwtUiBinder.java";
  public static final @NonNls String LAYOUT_UI_XML = "GwtLayout.ui.xml";
  public static final @NonNls String UI_RENDERER_JAVA = "GwtUiRenderer.java";
  public static final @NonNls String UI_RENDERER_CONTENT_JAVA = "GwtUiRendererContent.java";
  public static final @NonNls String UI_RENDERER_LAYOUT_UI_XML = "GwtUiRendererLayout.ui.xml";
  public static final @NonNls String EVENT_JAVA = "GwtEvent.java";
  public static final @NonNls String EVENT_HANDLER_JAVA = "GwtEventHandler.java";

  public static final @NonNls String[] TEMPLATES = {
    GWT_MODULE_CSS, GWT_MODULE_1_4_GWT_XML, GWT_MODULE_1_5_GWT_XML, GWT_MODULE_1_6_GWT_XML,
    GWT_MODULE_HTML, GWT_MODULE_HTML_1_4, GWT_MODULE_HTML_1_6,
    GWT_SERVICE_JAVA, GWT_SERVICE_JAVA_1_0, GWT_SERVICE_JAVA_1_5,
    GWT_SERVICE_ASYNC_JAVA, GWT_SERVICE_IMPL_JAVA, GWT_ENTRY_POINT_JAVA,

    GWT_SERIAL_CLASS_JAVA, GWT_TEST_CASE_JAVA,

    UI_BINDER_JAVA, LAYOUT_UI_XML,

    UI_RENDERER_JAVA, UI_RENDERER_CONTENT_JAVA, UI_RENDERER_LAYOUT_UI_XML,

    EVENT_JAVA, EVENT_HANDLER_JAVA
  };
  public static final @NonNls String GWT_MODULE_DOCTYPE_VAR = "GWT_MODULE_DOCTYPE";

  private GwtTemplates() {
  }
}
