<%@ taglib prefix="tiles" uri="http://struts.apache.org/tags-tiles" %>

<tiles:insert definition="third" template="/index.jsp">
  <tiles:put name="header"/>
  <tiles:put name="footer"/>
  <tiles:put name="<error>notExistingPut</error>"/>
</tiles:insert>

<tiles:insert definition="<error>notExistingDefinition</error>">
</tiles:insert>

<tiles:insert definition="third" role="user"/>

<tiles:insert definition="third" controllerUrl="/index.jsp"/>
<tiles:insert definition="third" controllerUrl="<error>notExisting</error>"/>

<tiles:definition id="myLocalDefinition" >
  <tiles:put name="footer"  type="page" value="/index.jsp"/>
  <tiles:put name="invalidpath"  type="page" value="<error>notExisting</error>"/>
</tiles:definition>

<tiles:definition id="myLocalDefinition2" extends="third"/>
