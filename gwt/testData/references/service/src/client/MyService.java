package client;

@com.google.gwt.user.client.rpc.RemoteServiceRelativePath("/MyService1")
public interface MyService extends com.google.gwt.user.client.rpc.RemoteService {
   String method(int i);

    public static class App {
      private static final MyServiceAsync ourInstance;
      static {
        ourInstance = (MyServiceAsync) com.google.gwt.core.client.GWT.create(MyService.class);
        ((com.google.gwt.user.client.rpc.ServiceDefTarget) ourInstance).setServiceEntryPoint(
           com.google.gwt.core.client.GWT.getModuleBaseURL() + "/MyService2");
      }

      public static MyServiceAsync getInstance() {
        return ourInstance;
      }
    }

}