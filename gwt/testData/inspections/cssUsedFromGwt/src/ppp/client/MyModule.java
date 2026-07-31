package ppp.client;

import com.google.gwt.user.client.ui.Button;

public class MyModule {
  public void init() {
    Button button = new Button();
    button.addStyleName("RedButton");
    button.addStyleName("GreenButton RedButton");
  }
}