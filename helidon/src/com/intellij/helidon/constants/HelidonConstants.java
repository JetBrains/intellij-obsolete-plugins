// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.constants;

import org.jetbrains.annotations.NonNls;

public interface HelidonConstants {
   @NonNls String ROUTING = "io.helidon.webserver.Routing";
   @NonNls String ROUTING_BUILDER = "io.helidon.webserver.Routing.Builder";
   @NonNls String ROUTING_RULES = "io.helidon.webserver.Routing.Rules";
   @NonNls String SERVICE = "io.helidon.webserver.Service";
   @NonNls String HANDLER = "io.helidon.webserver.Handler";
   @NonNls String HTTP_REQUEST_PATH = "io.helidon.common.http.HttpRequest.Path";
   @NonNls String HTTP_SERVER_REQUEST = "io.helidon.webserver.ServerRequest";
   @NonNls String HTTP_SERVER_RESPONSE = "io.helidon.webserver.ServerResponse";

  @NonNls String MP_MAIN = "io.helidon.microprofile.cdi.Main";
}
