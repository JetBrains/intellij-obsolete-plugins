package client;

public interface MyCssResource extends BaseResource {
  @ClassName("my-class")
  String myButtonClass();

  @ClassName("my-unresolved-class-1")
  String myButtonClass();

  String myUnresolvedClass2();

  String myLabelClass();

  String insideIf();
}