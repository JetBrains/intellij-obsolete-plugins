class Product {

  String name
  String date
  String quality
  String weight

  public String getSize() {
    return "asda"
  }

  public void setSize(String size) {

  }

  int transientField1;
  String transientField2; 

  static transients = ['transientField1', "transientField2"]

}