import com.google.gwt.user.client.rpc.*;

public interface MyServiceAsyncInterface<T> {
  void method(AsyncCallback<T> c);
}