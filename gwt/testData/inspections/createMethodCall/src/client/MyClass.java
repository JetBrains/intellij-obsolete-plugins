package client;

import com.google.gwt.core.client.GWT;

public class MyClass {
  public void m() {
    GWT.create(MyService.class);
    GWT.create();
    GWT.create(null);
    GWT.create(1);
    GWT.create(1+2);
    GWT.create(a);
    Class b = MyService.class;
    GWT.create(b);
  }
}