<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<html:form action="/login" onsubmit="<warn>unknown<warn>" onreset="document.createComment('bla-bla')">

</html:form>

<% String i = null; %>
<html:text name="manualInvoiceForm"
                                                   property='<%="NPCharges["+i+"].seriesStart"%>'
                                                   styleId='<%="seriesStart" + i%>'
                                                   maxlength="11"
                                                   size="11"
                                                   style="TEXT-TRANSFORM: uppercase"
                                                   onfocus='<%="onBlurSeriesStart(this)"%>'
                                                   onchange='<%="onChangeSeriesStart(this, " + i + ")"%>'/>