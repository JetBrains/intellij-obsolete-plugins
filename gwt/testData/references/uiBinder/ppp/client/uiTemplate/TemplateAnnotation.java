package ppp.client.uiTemplate;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.*;

public class TemplateAnnotation {
  @UiTemplate("Temp<caret>late.ui.xml")
  interface MyUiBinder extends UiBinder<DivElement, TemplateAnnotation> {
  }
}
