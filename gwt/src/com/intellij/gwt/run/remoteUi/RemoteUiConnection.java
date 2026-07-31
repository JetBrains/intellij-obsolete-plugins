package com.intellij.gwt.run.remoteUi;

import com.intellij.gwt.run.GwtRunConfiguration;
import com.intellij.gwt.run.remoteUi.responses.CapabilityExchangeResponse;
import com.intellij.gwt.run.remoteUi.responses.InitializeResponse;
import com.intellij.gwt.run.remoteUi.responses.RemoteUiResponse;
import com.intellij.gwt.run.remoteUi.responses.RemoteUiResponseReader;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.ide.browsers.JavaScriptDebuggerStarter;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.util.io.socketConnection.AbstractResponseHandler;
import com.intellij.util.io.socketConnection.RequestResponseExternalizerFactory;
import com.intellij.util.io.socketConnection.RequestWriter;
import com.intellij.util.io.socketConnection.ResponseReader;
import com.intellij.util.io.socketConnection.SocketConnection;
import com.intellij.util.io.socketConnection.SocketConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class RemoteUiConnection {
  private final SocketConnection<RemoteUiRequest, RemoteUiResponse> myConnection;
  private final GwtRunConfiguration myConfiguration;
  private final GwtVersion mySdkVersion;
  private final boolean myOpenInBrowser;
  private final boolean myStartJavaScriptDebugger;
  private final WebBrowser myBrowser;

  public RemoteUiConnection(GwtRunConfiguration configuration, @NotNull GwtVersion sdkVersion, boolean openInBrowser,
                            boolean startJavaScriptDebugger, @Nullable WebBrowser browser) {
    myConfiguration = configuration;
    mySdkVersion = sdkVersion;
    myOpenInBrowser = openInBrowser;
    myStartJavaScriptDebugger = startJavaScriptDebugger;
    myBrowser = browser;
    myConnection = SocketConnectionFactory.createServerConnection(7901, null,20, new RemoteUiRequestExternalizer());
    myConnection.registerHandler(CapabilityExchangeResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull CapabilityExchangeResponse response) {
        myConnection.sendRequest(RemoteUiRequest.createCapabilitiesResponse(response.getMessageId()));
      }
    });
    myConnection.registerHandler(InitializeResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull InitializeResponse response) {
        final List<String> urls = response.getStartupUrlsList();
        if (!urls.isEmpty()) {
          final String url = urls.get(0);
          //noinspection SSBasedInspection
          SwingUtilities.invokeLater(() -> onInitialized(url));
        }
        myConnection.sendRequest(RemoteUiRequest.createDummyResponseRequest(response.getMessageId()));
      }
    });
  }

  public void sendRestartServerRequest() {
    myConnection.sendRequest(RemoteUiRequest.createRestartServerRequest());
  }

  public @NotNull GwtVersion getSdkVersion() {
    return mySdkVersion;
  }

  private void onInitialized(String url) {
    if (myOpenInBrowser) {
      JavaScriptDebuggerStarter.Util.startDebugOrLaunchBrowser(myConfiguration, url, myBrowser.getId().toString(),
                                                               myStartJavaScriptDebugger);
    }
  }

  public int open() throws IOException {
    myConnection.open();
    return myConnection.getPort();
  }

  public SocketConnection<RemoteUiRequest, RemoteUiResponse> getConnection() {
    return myConnection;
  }

  public void close() {
    myConnection.close();
  }

  private static class RemoteUiRequestExternalizer extends RequestResponseExternalizerFactory<RemoteUiRequest, RemoteUiResponse> {
    @Override
    public @NotNull RequestWriter<RemoteUiRequest> createRequestWriter(final @NotNull OutputStream output) throws IOException {
      return new RequestWriter<>() {
        @Override
        public void writeRequest(RemoteUiRequest request) throws IOException {
          request.sendMessage(output);
        }
      };
    }

    @Override
    public @NotNull ResponseReader<RemoteUiResponse> createResponseReader(@NotNull InputStream input) throws IOException {
      return new RemoteUiResponseReader(input);
    }
  }
}
