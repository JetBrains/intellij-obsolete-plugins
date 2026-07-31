import com.google.gwt.user.client.rpc.*;

public interface MyServiceAsync {
  void method(AsyncCallback<String> c);
}