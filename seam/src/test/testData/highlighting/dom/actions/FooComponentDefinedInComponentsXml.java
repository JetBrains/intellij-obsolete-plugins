package actions;

import org.jboss.seam.annotations.Name;

class FooComponentDefinedInComponentsXml {

    @DataModel
    private List<String> strings;

    @Factory("stringsFactory")
    public List<String> createMessages() {return null;}
}