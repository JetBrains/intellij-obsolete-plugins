package xxx.client.attributes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class WithUiFactory extends Composite {

    interface WithUiFactoryUiBinder extends UiBinder<HTMLPanel, WithUiFactory> { }

    private static WithUiFactoryUiBinder ourUiBinder = GWT.create(WithUiFactoryUiBinder.class);

    public WithUiFactory() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiFactory
    public MyWidget createMyWidget() {
        return new MyWidget(null);
    }
}