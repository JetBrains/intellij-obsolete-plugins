package xxx.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MyServiceAsync {
  void calc(AsyncCallback<String> async);
}