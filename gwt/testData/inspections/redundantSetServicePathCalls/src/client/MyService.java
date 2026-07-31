import com.google.gwt.user.client.rpc.*;
import com.google.gwt.core.client.GWT;

public interface MyService extends com.google.gwt.user.client.rpc.RemoteService {

    public static class App {
      private static final MyServiceAsync ourInstance;
      static {
        ourInstance = (MyServiceAsync) GWT.create(MyService.class);
        ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "myservicepath");
      }

      public static MyServiceAsync getInstance() {
        return ourInstance;
      }
    }
}

interface MyServiceAsync {
}