import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;
import java.util.*;

public class MyModule {
    public void method() {
        Button b = new Button();
        <caret>ClickListener listener = new ClickListener() {
            public void onClick(Widget sender) {
                Window.alert("123");
            }
        };
        b.addClickListener(listener);
        List x = new ArrayList();
        x.add(listener);
    }
}
