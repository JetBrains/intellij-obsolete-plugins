package ppp.client.uiTemplate;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.client.UiTemplate;

public class MyComponentOuterFull extends AbstractCell<String> {

  private static MyFullUiRenderer myRenderer = GWT.create(MyFullUiRenderer.class);

  @Override
  public void render(Context context, String value, SafeHtmlBuilder builder) {
    myRenderer.render(builder, value);
  }
}

@UiTemplate("ppp.client.uiTemplate.MyComponent.ui.xml")
interface MyFullUiRenderer extends UiRenderer {
  void render(SafeHtmlBuilder sb, String entity);
}
