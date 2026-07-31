package client;

import com.google.gwt.resources.client.CssResource; import com.google.gwt.uibinder.client.*;

public class MyWidget {
  interface MyStyle extends CssResource {
    @ClassName("my-class")
    String myButtonClass();

    @ClassName("my-unresolved-class-1")
    String myButtonClass();

    String myUnresolvedClass2();

    String myLabelClass();
  }

  interface MyUiBinder extends UiBinder<DivElement, MyWidget> {}
}