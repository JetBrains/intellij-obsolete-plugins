import org.jboss.seam.annotations.*;

@Name("annoEventRaiser")
public class ObserverRaiseEventAnno {

  @RaiseEvent("eventType_1")
  public void doSomething(){}

  @RaiseEvent("eventType_2")
  public void doSomething(){}
}