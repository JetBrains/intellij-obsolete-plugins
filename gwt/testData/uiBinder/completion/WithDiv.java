
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

public class WithDiv {

    @UiTemplate("pkg.Layout.ui.xml")
    interface WithDivUiBinder extends UiBinder<DivElement, WithDiv> {}

    private static WithDivUiBinder ourUiBinder = GWT.create(WithDivUiBinder.class);

    @UiField
    DivElement the<caret>;

    public Sample() {
        ourUiBinder.createAndBindUi(this);
    }
}