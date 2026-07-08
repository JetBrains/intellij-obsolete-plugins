define myns::sometype{

}

myns::sometype{
  'instance':
}

Myns::Sometype[instance]
Myns::Sometype['instance']

Resource[Myns::Sometype, instance, 'instance']

Resource[Myns::Sometype, 'ins<caret>tance']{
  name => 42
}

