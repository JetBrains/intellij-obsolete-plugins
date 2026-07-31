package ppp.client.uiTemplate;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.client.UiTemplate;

public class MyComponentFull extends AbstractCell<String> {

  @UiTemplate("ppp.client.uiTemplate.MyComponent.ui.xml")
  interface MyUiRenderer extends UiRenderer {
    void render(SafeHtmlBuilder sb, String entity);
  }

  private static MyUiRenderer myRenderer = GWT.create(MyUiRenderer.class);

  @Override
  public void render(Context context, String value, SafeHtmlBuilder builder) {
    myRenderer.render(builder, value);
  }
}
