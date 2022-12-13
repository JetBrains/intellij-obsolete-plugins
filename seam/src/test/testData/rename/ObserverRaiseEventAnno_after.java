import org.jboss.seam.annotations.*;

@Name("annoEventRaiser")
public class ObserverRaiseEventAnno {

  @RaiseEvent("new_event_type")
  public void doSomething(){}

  @RaiseEvent("eventType_2")
  public void doSomething(){}
}