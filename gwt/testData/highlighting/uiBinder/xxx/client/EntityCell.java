package xxx.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.*;

public class EntityCell extends AbstractCell<String> {

    @UiTemplate("EntityCell.ui.xml")
    interface EntityCellUiRenderer extends UiRenderer {
        void render(SafeHtmlBuilder sb, String entity);
        void onBrowserEvent(EntityCell c, NativeEvent e, Element p, String entity);
    }

    @Override
    public void render(Context context, String val, SafeHtmlBuilder builder){
        ourRenderer.render(builder, val);
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value,
                               NativeEvent event, ValueUpdater<String> updater) {
        ourRenderer.onBrowserEvent(this, event, parent, value);
    }

    public EntityCell(){
        super("click", "dblclick");
    }

    @UiHandler("entity")
    void onClick(ClickEvent event, Element parent, String value) {}

    @UiHandler("entity")
    void onDoubleClick(DoubleClickEvent event) {}

    @UiHandler("entity")
    void <error descr="@UiHandler 'onSomething()' must have at least one parameter defined">onSomething</error>() {}

    private static EntityCellUiRenderer ourRenderer = GWT.create(EntityCellUiRenderer.class);
}

