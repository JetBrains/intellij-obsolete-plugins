package client;

import com.google.gwt.resources.client.CssResource;

public interface MyCssResource extends CssResource {
  @ClassName("my-class<caret>")
  String myButtonClass();
}