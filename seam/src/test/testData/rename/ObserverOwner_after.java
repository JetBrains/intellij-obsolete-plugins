import org.jboss.seam.annotations.*;

@Name("observerOwner")
public class ObserverOwner {
  @Observer("new_event_type")
  public void doSomething(){}

  @Observer({"eventType_2", "new_event_type"})
  public void doSomething2(){}

  @Observer({"new_event_type",  "eventType_2", "eventType_3"})
  public void doSomething2(){}
}
