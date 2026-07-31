import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        Button button = new Button("aaa");
        MouseDownHandler listener = new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent mouseDownEvent) {
                System.out.println("239");
            }
        };
        button.addMouseDownHandler(listener);
    }
}
