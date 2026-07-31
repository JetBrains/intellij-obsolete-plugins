package ppp.client.uiWith;

import com.google.gwt.resources.client.*;

public interface MyResources extends ClientBundle {
  @Source("style.css")
  Style style();

  @Source("Logo.jpg")
  ImageResource logo();
  @Source("Logo2.jpg")
  ImageResource logo2();

  public interface Style extends CssResource {
    String mainBlock();
    String mainBlock2();
  }
}