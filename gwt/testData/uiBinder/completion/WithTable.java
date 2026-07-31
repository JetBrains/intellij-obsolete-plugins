
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

public class WithDiv {

    @UiTemplate("pkg.Layout.ui.xml")
    interface WithDivUiBinder extends UiBinder<DivElement, WithDiv> {}

    private static WithDivUiBinder ourUiBinder = GWT.create(WithDivUiBinder.class);

    @UiField
    TableElement the<caret>;

    public Sample() {
        ourUiBinder.createAndBindUi(this);
    }
}