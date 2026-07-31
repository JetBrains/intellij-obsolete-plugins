package pkg;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiRenderer;

public class MyComponent extends AbstractCell<Object> {

    interface MyComponentUiRenderer extends UiRenderer {
        void render(SafeHtmlBuilder sb, Object value);

        void onBrowserEvent(MyComponent cell, NativeEvent event, Element parent, Object value);
    }

    private static MyComponentUiRenderer ourUiRenderer = GWT.create(MyComponentUiRenderer.class);

    public MyComponent() {
        super();
    }

    @Override
    public void render(Context context, Object value, SafeHtmlBuilder builder) {
        ourUiRenderer.render(builder, value);
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, Object value,
                               NativeEvent event, ValueUpdater<Object> updater) {
        ourUiRenderer.onBrowserEvent(this, event, parent, value);
    }
}