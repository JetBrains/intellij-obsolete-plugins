package xxx.client;

import com.google.gwt.user.client.rpc.*;

public interface SampleServiceAsyncBase {
  void print(AsyncCallback<Void> c);
}