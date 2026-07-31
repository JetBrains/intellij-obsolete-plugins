package xxx.client.attributes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class WithParameterizedUiFactory extends Composite {

    interface WithParameterizedUiFactoryUiBinder extends UiBinder<HTMLPanel, WithParameterizedUiFactory> { }

    private static WithParameterizedUiFactoryUiBinder ourUiBinder = GWT.create(WithParameterizedUiFactoryUiBinder.class);

    public WithParameterizedUiFactory() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiFactory
    public MyWidget createMyWidget(String factoryParam) {
        return new MyWidget(null);
    }
}