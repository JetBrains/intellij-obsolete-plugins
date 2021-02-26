package com.intellij.restClient.php;

import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.restClient.RESTClient;
import com.intellij.restClient.RestClientLegacyBundle;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.httpClient.http.request.run.HttpClientRequestProcessHandler;
import com.intellij.httpClient.execution.RestClientRequest;
import com.intellij.httpClient.execution.RestClientRequestProcessor;
import com.jetbrains.php.restClient.PhpXDebugHttpRequestDebugger;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.cookie.Cookie;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


public class PhpSendRequestInDebugMode extends AnAction {
  private static final Logger LOG = Logger.getInstance(PhpSendRequestInDebugMode.class);

  private final RESTClient myRESTClient;

  public PhpSendRequestInDebugMode(@NotNull RESTClient client) {
    super(RestClientLegacyBundle.message("debug.rest.client.submit.request.in.debug.mode"),
            RestClientLegacyBundle.message("debug.rest.client.runs.request.in.debug.mode"),
          AllIcons.Toolwindows.ToolWindowDebugger);
    myRESTClient = client;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }


    URI uri;
    String url = myRESTClient.getRequestURL();
    try {
      uri = new URI(url);
    }
    catch (URISyntaxException ex) {
      LOG.error(RestClientLegacyBundle.message("debug.rest.client.can.not.resolve.host.name", url));
      return;
    }

    final HttpHost host = URIUtils.extractHost(uri);
    if (host == null) {
      final String message = RestClientLegacyBundle.message("debug.rest.client.can.not.resolve.host.name", url);
      LOG.error(message);
      showErrorDialog(project, message);
      return;
    }

    final HttpClientRequestProcessHandler executeProcessHandler = new HttpClientRequestProcessHandler(false);
    try {
      //TODO: support zend debugger + add ability to choose debugger before sending request
      final PhpXDebugHttpRequestDebugger debugger = new PhpXDebugHttpRequestDebugger();
      final String hostName = host.getHostName();
      // TODO: wait for eap
      final RestClientRequest.Biscuit debugCookie = debugger.startDebugSessionAndCreateCookies(project, hostName, executeProcessHandler);
      myRESTClient.onGoToUrlAction(new RestClientRequestProcessor() {
        @Override
        public void preProcessRequest(@NotNull RestClientRequest request) {
          request.biscuits.add(debugCookie);
        }

        @Override
        public void postProcessRequest(@NotNull RestClientRequest request, @NotNull CookieStore store) {
          request.biscuits.remove(debugCookie);

          final List<Cookie> filtered = ContainerUtil.filter(store.getCookies(),
                                                             cookie -> !StringUtil.equals(debugCookie.getName(), cookie.getName()));
          store.clear();

          for (Cookie cookie : filtered) {
            store.addCookie(cookie);
          }
          executeProcessHandler.onRunFinished();
        }
      });
    }
    catch (ExecutionException e1) {
      LOG.error(e1);
      showErrorDialog(project, e1.getMessage());
    }
  }

  private static void showErrorDialog(@NotNull Project project, @NotNull @Nls String message) {
    Messages.showErrorDialog(project, message, RestClientLegacyBundle.message("debug.rest.client.can.not.resolve.host.name.title"));
  }
}
