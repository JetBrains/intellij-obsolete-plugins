import com.google.gwt.user.client.rpc.*;

public interface MyServiceAsync {
  void method1(String f, AsyncCallback c);
  void method2(int i, AsyncCallback c);
  void method3(AsyncCallback<Integer> c);
  void method4();
}