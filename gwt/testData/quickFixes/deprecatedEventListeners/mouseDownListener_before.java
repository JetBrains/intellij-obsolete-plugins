import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        Button button = new Button("aaa");
        <caret>MouseListenerAdapter listener = new MouseListenerAdapter() {
            @Override
            public void onMouseDown(Widget sender, int x, int y) {
                System.out.println("239");
            }
        };
        button.addMouseListener(listener);
    }
}
