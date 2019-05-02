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

package com.intellij.struts;

import org.jetbrains.annotations.NonNls;

/**
 * @author Dmitry Avdeev
 */
public interface StrutsConstants {

  String PLUGIN_NAME = "Struts Assistant";

  @NonNls
  String[] STRUTS_DTDS = new String[]{

    "http://jakarta.apache.org/struts/dtds/struts-config_1_0.dtd",
    "http://jakarta.apache.org/struts/dtds/struts-config_1_1.dtd",
    "http://jakarta.apache.org/struts/dtds/struts-config_1_2.dtd",
    "http://jakarta.apache.org/struts/dtds/struts-config_1_3.dtd",

    "http://struts.apache.org/dtds/struts-config_1_0.dtd",
    "http://struts.apache.org/dtds/struts-config_1_1.dtd",
    "http://struts.apache.org/dtds/struts-config_1_2.dtd",
    "http://struts.apache.org/dtds/struts-config_1_3.dtd",

    "-//Apache Software Foundation//DTD Struts Configuration 1.1//EN",
    "-//Apache Software Foundation//DTD Struts Configuration 1.2//EN",
    "-//Apache Software Foundation//DTD Struts Configuration 1.3//EN"
  };

  @NonNls
  String[] TILES_DTDS = new String[]{
    "http://jakarta.apache.org/struts/dtds/tiles-config.dtd",
    "http://jakarta.apache.org/struts/dtds/tiles-config_1_1.dtd",
    "http://jakarta.apache.org/struts/dtds/tiles-config_1_3.dtd",
      
    "http://struts.apache.org/dtds/tiles-config.dtd",
    "http://struts.apache.org/dtds/tiles-config_1_1.dtd",
    "http://struts.apache.org/dtds/tiles-config_1_3.dtd",

    "http://tiles.apache.org/dtds/tiles-config_2_0.dtd",
    "http://tiles.apache.org/dtds/tiles-config_2_1.dtd",

    "http://tiles.apache.org/dtds/tiles-config_3_0.dtd",

    "-//Apache Software Foundation//DTD Tiles Configuration 1.1//EN",
    "-//Apache Software Foundation//DTD Tiles Configuration 1.2//EN",
    "-//Apache Software Foundation//DTD Tiles Configuration 1.3//EN",
    "-//Apache Software Foundation//DTD Tiles Configuration 2.0//EN",
    "-//Apache Software Foundation//DTD Tiles Configuration 2.1//EN",
    "-//Apache Software Foundation//DTD Tiles Configuration 3.0//EN"
  };

  @NonNls
  String TILES_DOM_NAMESPACE_KEY = "tiles.domnamespace";

  @NonNls
  String[] VALIDATOR_DTDS = new String[]{
    "http://jakarta.apache.org/commons/dtds/validator_1_0.dtd",
    "http://jakarta.apache.org/commons/dtds/validator_1_0_1.dtd",
    "http://jakarta.apache.org/commons/dtds/validator_1_1.dtd",
    "http://jakarta.apache.org/commons/dtds/validator_1_1_3.dtd",
    "http://jakarta.apache.org/commons/dtds/validator_1_2_0.dtd",
    "http://jakarta.apache.org/commons/dtds/validator_1_3_0.dtd",

    "-//Apache Software Foundation//DTD Commons Validator Rules Configuration 1.0//EN",
    "-//Apache Software Foundation//DTD Commons Validator Rules Configuration 1.0.1//EN",
    "-//Apache Software Foundation//DTD Commons Validator Rules Configuration 1.1//EN",
    "-//Apache Software Foundation//DTD Commons Validator Rules Configuration 1.1.3//EN",
    "-//Apache Software Foundation//DTD Commons Validator Rules Configuration 1.3.0//EN"

  };

  @NonNls String ACTION_SERVLET_CLASS = "org.apache.struts.action.ActionServlet";
}