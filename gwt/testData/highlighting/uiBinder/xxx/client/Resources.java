package xxx.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.resources.client.*;


public interface Resources extends ClientBundle {
  @Source("Style.css")
  Style style();

  @Source("Logo.jpg")
  ImageResource logo();

  public interface Style extends CssResource {
    String mainBlock();
    String nameSpan();
    Sprite userPictureSprite();
  }
}