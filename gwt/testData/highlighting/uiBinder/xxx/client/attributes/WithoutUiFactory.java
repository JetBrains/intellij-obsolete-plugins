package xxx.client.attributes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class WithoutUiFactory extends Composite {

    interface WithoutUiFactoryUiBinder extends UiBinder<HTMLPanel, WithoutUiFactory> { }

    private static WithoutUiFactoryUiBinder ourUiBinder = GWT.create(WithoutUiFactoryUiBinder.class);

    public WithoutUiFactory() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}