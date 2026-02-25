class TestController {

  static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

  def getAction1() {
    return {render "content"}
  }

  Closure getAction2() {
    return {render "content"}
  }

  public Closure getAction3() {
    return {render "content"}
  }

  private Closure getNotAction1() {
    return {render "content"}
  }

  protected Closure getNotAction2() {
    return {render "content"}
  }

  static Closure getNotAction961489() {
    return {render "content"}
  }

  String getNotAction3() {
    return "";
  }

  def Closure action4 = {
    render "eeeee"
  }, action5 = {
    render "uuu"
  }

  def action6 = {
    render "eeeee"
  }, notAction435 = "assa";

  def action7 = {
    redirect(action: "list", params: params)
  }

  Closure action8 = {

  }

  public Closure notAction5694 = {
    render "asda"
  }

  protected Closure notAction927 = {
    render "asda"
  }

  private Closure notAction926 = {
    render "asda"
  }

  transient final Closure action9 = {
    render "asda"
  }

}
