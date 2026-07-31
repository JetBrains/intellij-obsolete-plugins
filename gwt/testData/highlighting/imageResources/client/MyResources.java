package client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource;

public interface MyResources extends ClientBundle {
  ImageResource img1();

  ImageResource img2();

  @Source("style.css")
  CssResource <warning descr="Method 'style()' is never used">style</warning>();
}