define myns::sometype{

}

myns::som<caret>etype{
  'instance':
}

Myns::Sometype[instance]

Resource[Myns::Sometype, instance]

Type[Myns::Sometype]

Myns::Sometype{
  name => 'test'
}

class myns::someclass{
  $somevar = 1
}

class myns::otherclass inherits Myns::Someclass{

}

function myns::somefunc{

}

notice $myns::someclass::somevar