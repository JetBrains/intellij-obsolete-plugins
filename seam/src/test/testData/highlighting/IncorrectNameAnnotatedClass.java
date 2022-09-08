import org.jboss.seam.annotations.Name;

<error>@Name("interface")</error>
interface IncorrectInterface {
}

<error>@Name("abstractClass")</error>
abstract class IncorrectAbstarctClass {
}

<error>@Name("noEmptyConstructor")</error>
class NoEmptyConstructorClass {
  public NoEmptyConstructorClass(int a) {}
}
