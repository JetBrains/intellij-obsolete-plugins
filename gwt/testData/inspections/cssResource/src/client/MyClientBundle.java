package client;

import com.google.gwt.resources.client.ClientBundle;

public interface MyClientBundle extends ClientBundle {
  @Source("app.css")
  MyCssResource css();

  @Source("def.css")
  MyDef def();
}