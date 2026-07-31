package client;

import com.google.gwt.user.client.ui.*;

public class MyModule {
  public void method() {
    RootPanel.get("gwtSlot").add(new Button());
  }
}