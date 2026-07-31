import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;

public class MyModule {
    public void method() {
        Button b = new Button();
        b.addClickListener(new <caret>ClickListener() {
            public void onClick(Widget sender) {
                Window.alert("123");
            }
        });
    }
}
