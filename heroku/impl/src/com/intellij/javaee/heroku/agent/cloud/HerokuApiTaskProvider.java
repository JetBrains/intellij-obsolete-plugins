package com.intellij.javaee.heroku.agent.cloud;

import com.heroku.api.HerokuAPI;
import com.heroku.api.connection.JerseyClientAsyncConnection;
import com.heroku.api.exception.HerokuAPIException;
import com.intellij.remoteServer.agent.util.CloudAgentErrorHandler;
import com.intellij.remoteServer.agent.util.CloudAgentLogger;

import java.io.IOException;

/**
 * @author michael.golubev
 */
public class HerokuApiTaskProvider {

  private static final long REQUEST_DELAY_MSEC = 6000;

  private static final int RATE_LIMIT_LOW = 100;

  private final HerokuAPI myApi;
  private final CloudAgentErrorHandler myErrorHandler;
  private final CloudAgentLogger myLogger;

  private long myLastRequestTime;
  private int myRemainingRateLimit;

  public HerokuApiTaskProvider(HerokuAPI api, CloudAgentErrorHandler errorHandler, CloudAgentLogger logger) {
    myApi = api;
    myErrorHandler = errorHandler;
    myLogger = logger;
  }

  public abstract class ApiTask<T> {

    public T perform() {
      long currentTime = System.currentTimeMillis();
      if (myRemainingRateLimit < RATE_LIMIT_LOW) {
        long deltaTime = currentTime - myLastRequestTime;
        if (deltaTime < REQUEST_DELAY_MSEC) {
          try {
            Thread.sleep(REQUEST_DELAY_MSEC - deltaTime);
          }
          catch (InterruptedException e) {
            myLogger.debugEx(e);
          }
        }
      }
      myLastRequestTime = currentTime;

      try {
        return doPerform(myApi);
      }
      catch (HerokuAPIException ex) {
        onError(ex);
        return null;
      }
      catch (IOException ex) {
        onError(ex);
        return null;
      }
      finally {
        Integer remainingRateLimit = ((JerseyClientAsyncConnection)myApi.getConnection()).getRemainingRateLimit();
        myRemainingRateLimit = remainingRateLimit == null ? Integer.MAX_VALUE : remainingRateLimit;
        myLogger.debug("remaining rate limit: " + myRemainingRateLimit);
      }
    }

    protected abstract T doPerform(HerokuAPI api) throws HerokuAPIException, IOException;

    protected void onError(Exception ex) {
      myErrorHandler.onError(ex.toString());
    }
  }

  public abstract class ApiSilentTask<T> extends ApiTask<T> {

    @Override
    protected void onError(Exception ex) {
      myLogger.debugEx(ex);
    }
  }
}
