import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.user.client.ui.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        MouseWheelHandler listener = new MyMouseWheelListener();
        FocusPanel panel = new FocusPanel();
        panel.addMouseWheelHandler(listener);
    }

    private static class MyMouseWheelListener implements MouseWheelHandler {
        public void onMouseWheel(MouseWheelEvent mouseWheelEvent) {
            System.out.println("239");
        }
    }
}
