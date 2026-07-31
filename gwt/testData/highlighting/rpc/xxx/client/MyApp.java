package xxx.client;

import com.google.gwt.core.client.GWT;

public class MyApp {
  public static void main(String[] args) {
    MyServiceAsync service = GWT.create(MyService.class);
    service.calc(null);
  }
}