package ppp.client.inheritance;

import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;

public class MyBaseComponent {
  @UiField SpanElement nameSpanField;
  @UiField SpanElement nameSpanField2;
  SpanElement nameSpanField3;

  @UiField Button myButtonField;
  @UiField Button myButtonField2;
  Button myButtonField3;

}