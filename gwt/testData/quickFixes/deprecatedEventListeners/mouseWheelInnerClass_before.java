import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        <caret>MouseWheelListener listener = new MyMouseWheelListener();
        FocusPanel panel = new FocusPanel();
        panel.addMouseWheelListener(listener);
    }

    private static class MyMouseWheelListener implements MouseWheelListener {
        public void onMouseWheel(Widget sender, MouseWheelVelocity velocity) {
            System.out.println("239");
        }
    }
}
