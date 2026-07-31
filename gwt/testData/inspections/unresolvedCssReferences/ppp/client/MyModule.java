package ppp.client;

import com.google.gwt.user.client.ui.Button;

public class MyModule {
  public void init() {
    Button button = new Button();
    button.addStyleName("UnknownStyle");
    button.addStyleName("GreenButton");
    button.addStyleName("GreenButton UnknownStyle");
  }
}