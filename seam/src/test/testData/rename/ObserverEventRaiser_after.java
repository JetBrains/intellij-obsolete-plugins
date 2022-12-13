import org.jboss.seam.core.*;

public class ObserverEventRaiser {
  private Events events;

  public void doSomething(){
    events.raiseEvent("new_event_type");
    events.raiseAsynchronousEvent("new_event_type");
    events.raiseTransactionSuccessEvent("new_event_type");
    events.raiseTimedEvent("new_event_type");
    events.raiseTransactionCompletionEvent("new_event_type");
  }

  public void doSomething2(){
      events.raiseEvent("eventType_2");
      events.raiseAsynchronousEvent("eventType_2");
      events.raiseTransactionSuccessEvent("eventType_3");
      events.raiseTimedEvent("eventType_4");
      events.raiseTransactionCompletionEvent("eventType_5");
  }
}