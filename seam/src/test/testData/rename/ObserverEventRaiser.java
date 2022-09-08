import org.jboss.seam.annotations.*;
import org.jboss.seam.core.*;

public class ObserverEventRaiser {
  private Events events;

  public void doSomething(){
    events.raiseEvent("eventType_1");
    events.raiseAsynchronousEvent("eventType_1");
    events.raiseTransactionSuccessEvent("eventType_1");
    events.raiseTimedEvent("eventType_1");
    events.raiseTransactionCompletionEvent("eventType_1");
  }

  public void doSomething2(){
      events.raiseEvent("eventType_2");
      events.raiseAsynchronousEvent("eventType_2");
      events.raiseTransactionSuccessEvent("eventType_3");
      events.raiseTimedEvent("eventType_4");
      events.raiseTransactionCompletionEvent("eventType_5");
  }
}