package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

public class MyModule implements EntryPoint {

  @Override
  public void onModuleLoad() {
    RootPanel.get("hae<caret>dIp");
  }
}