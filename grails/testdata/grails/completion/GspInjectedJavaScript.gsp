<html>
<head>
<g:javascript>
var counter = 0;
function updateItems()
{
new Ajax.Request('/protoTest/page/time/' + counter,
{
onSuccess:
function(e) {
time.innerHTML = resp.val + cou<caret>
updateItems();
}});
return false;
}
</g:javascript>
</head>
<body>
</body>
</html>