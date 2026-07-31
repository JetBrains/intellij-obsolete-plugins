package client;

import com.google.gwt.resources.client.ClientBundle;

public interface MyClientBundle extends ClientBundle {
  @Source("app.css")
  MyCssResource css();

  @Source("renameCssClass/methodName.css")
  client.renameCssClass.MethodName css2();

  @Source("renameMethod/methodName.css")
  client.renameMethod.MethodName css3();
}