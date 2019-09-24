package com.heroku.api.connection;

import com.heroku.api.Heroku;
import com.heroku.api.exception.HerokuAPIException;
import com.heroku.api.http.Http;
import com.heroku.api.request.Request;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.internal.util.Base64;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.heroku.api.Heroku.Config.ENDPOINT;

public class JerseyClientAsyncConnection implements AsyncConnection<Future<?>> {

  private final Client client;

  private Integer myRemainingRateLimit;

  public JerseyClientAsyncConnection() {
    this(ClientBuilder.newClient(createClientConfig()));
  }

  public JerseyClientAsyncConnection(Client client) {
    this.client = client;
  }

  @Override
  public <T> Future<T> executeAsync(final Request<T> request, final Map<String, String> extraHeaders, final String apiKey) {
    final Invocation.Builder builder = client.target(ENDPOINT.value + request.getEndpoint()).request();

    builder.header("Authorization", Base64.encodeAsString((":" + apiKey).getBytes(StandardCharsets.UTF_8)));
    builder.header(request.getResponseType().getHeaderName(), request.getResponseType().getHeaderValue());
    builder.header(Heroku.ApiVersion.v3.getHeaderName(), Heroku.ApiVersion.v3.getHeaderValue());
    builder.header(Http.ContentType.JSON.getHeaderName(), Http.ContentType.JSON.getHeaderValue());
    builder.header(Http.UserAgent.LATEST.getHeaderName(), Http.UserAgent.LATEST.getHeaderValue("jersey-client"));

    for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }

    final AsyncInvoker invoker = builder.async();

    final Future<Response> futureResponse = request.hasBody() ?
                                            invoker.method(request.getHttpMethod().name(), Entity.json(request.getBodyAsMap())) :
                                            invoker.method(request.getHttpMethod().name());

    return new Future<T>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return futureResponse.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
        return futureResponse.isCancelled();
      }

      @Override
      public boolean isDone() {
        return futureResponse.isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
        return handleResponse(futureResponse.get());
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return handleResponse(futureResponse.get(timeout, unit));
      }

      private T handleResponse(Response r) {
        return request.getResponse(r.readEntity(byte[].class), r.getStatus(), toJavaMap(r.getStringHeaders()));
      }
    };
  }

  @Override
  public <T> Future<T> executeAsync(Request<T> request, String apiKey) {
    final Invocation.Builder builder = client.target(ENDPOINT.value + request.getEndpoint()).request();

    builder.header("Authorization", Base64.encodeAsString((":" + apiKey).getBytes(StandardCharsets.UTF_8)));
    builder.header(request.getResponseType().getHeaderName(), request.getResponseType().getHeaderValue());
    builder.header(Heroku.ApiVersion.v3.getHeaderName(), Heroku.ApiVersion.v3.getHeaderValue());
    builder.header(Http.ContentType.JSON.getHeaderName(), Http.ContentType.JSON.getHeaderValue());
    builder.header(Http.UserAgent.LATEST.getHeaderName(), Http.UserAgent.LATEST.getHeaderValue("jersey-client"));

    for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }

    final AsyncInvoker invoker = builder.async();

    final Future<Response> futureResponse = request.hasBody() ?
                                            invoker.method(request.getHttpMethod().name(), Entity.json(request.getBodyAsMap())) :
                                            invoker.method(request.getHttpMethod().name());

    return new Future<T>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return futureResponse.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
        return futureResponse.isCancelled();
      }

      @Override
      public boolean isDone() {
        return futureResponse.isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
        return handleResponse(futureResponse.get());
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return handleResponse(futureResponse.get(timeout, unit));
      }

      private T handleResponse(Response r) {
        String header = (String)r.getHeaders().getFirst("X-RateLimit-Remaining");
        try {
          myRemainingRateLimit = header == null ? null : Integer.parseInt(header);
        }
        catch (NumberFormatException e) {
          myRemainingRateLimit = null;
        }
        return request.getResponse(r.readEntity(byte[].class), r.getStatus(), toJavaMap(r.getStringHeaders()));
      }
    };
  }

  @Override
  public <T> T execute(Request<T> request, String apiKey) {
    return execute(request, Collections.emptyMap(), apiKey);
  }

  @Override
  public <T> T execute(Request<T> request, Map<String, String> extraHeaders, String key) {
    try {
      return executeAsync(request, extraHeaders, key).get();
    }
    catch (InterruptedException e) {
      throw new HerokuAPIException(e);
    }
    catch (ExecutionException e) {
      throw new HerokuAPIException(e);
    }
  }

  @Override
  public void close() {
    client.close();
  }

  public Integer getRemainingRateLimit() {
    return myRemainingRateLimit;
  }

  private static <T> Future<Response> invoke(final Request<T> request, final Invocation.Builder builder) {
    if (Http.Method.PATCH.equals(request.getHttpMethod())) {
      final AsyncInvoker invoker = builder.async();
      builder.property("X-HTTP-Method-Override", "PATCH");
      return invoker.method("POST", Entity.json(request.getBodyAsMap()));
    }
    else {
      final AsyncInvoker invoker = builder.async();
      return request.hasBody() ?
             invoker.method(request.getHttpMethod().name(), Entity.json(request.getBodyAsMap())) :
             invoker.method(request.getHttpMethod().name());
    }
  }

  private static ClientConfig createClientConfig() {
    ClientConfig clientConfig = new ClientConfig();
    ConnectorProvider connectorProvider = new ApacheConnectorProvider();
    clientConfig.connectorProvider(connectorProvider);
    return clientConfig;
  }

  public static class Provider implements ConnectionProvider {
    @Override
    public Connection getConnection() {
      return new JerseyClientAsyncConnection();
    }
  }

  private static Map<String, String> toJavaMap(MultivaluedMap<String, String> headers) {
    Map<String, String> javaMap = new HashMap<>();
    for (String key : headers.keySet()) {
      javaMap.put(key, headers.getFirst(key));
    }
    return javaMap;
  }
}
