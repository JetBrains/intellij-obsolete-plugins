import org.jboss.seam.annotations.*;

@Name("observerOwner")
public class ObserverOwner {
  @Observer("eventType_1")
  public void doSomething(){}

  @Observer({"eventType_2",  "eventType_3"})
  public void doSomething2(){}

  @Observer({"eventType_1",  "eventType_4", "<caret>eventType_5"})
  public void doSomething2(){}
}
