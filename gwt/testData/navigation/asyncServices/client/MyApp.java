package client;

import com.google.gwt.core.client.GWT;

public class MyApp {
  {
    MyServiceAsync service = GWT.create(MyService.class);
    service.<caret>calc(null);
  }
}