import org.jboss.seam.annotations.*;
import org.jboss.seam.core.*;

public class ObserverEventRaiser {
  private Events events;

  public void doSomething(){
    events.raiseEvent("eventType_1");
    events.raiseAsynchronousEvent("eventType_2");
    events.raiseTransactionSuccessEvent("eventType_3");
    events.raiseTimedEvent("eventType_4");
    events.raiseTransactionCompletionEvent("<caret>eventType_5");
  }
}