import org.jboss.seam.annotations.*;

@Name("observerOwner")
public class ObserverOwner {
  @Observer("even<caret>tType_1")
  public void doSomething(){}

  @Observer({"eventType_2",  "eventType_1"})
  public void doSomething2(){}

  @Observer({"eventType_1",  "eventType_2", "eventType_3"})
  public void doSomething2(){}
}
