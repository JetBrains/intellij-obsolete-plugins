import org.jboss.seam.annotations.*;

@Name("annoEventRaiser")
public class ObserverRaiseEventAnno {

  @RaiseEvent("<caret>annoEventType_1")
  public void doSomething(){}

  @RaiseEvent("annoEventType_2")
  public void doSomething(){}
}