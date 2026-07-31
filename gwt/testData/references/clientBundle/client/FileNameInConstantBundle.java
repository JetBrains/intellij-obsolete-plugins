package client;

import com.google.gwt.resources.client.ClientBundle;

public interface FileNameInConstantBundle extends ClientBundle {
  String FILE_NAME = "app.css";

  @Source(FILE_NAME)
  MyCssResource <caret>css();
}