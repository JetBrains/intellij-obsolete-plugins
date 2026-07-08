define myns::sometype{

}

myns::sometype{
  supername:
}

Myns::Sometype[supername]
Myns::Sometype['supername']

Resource[Myns::Sometype, supername, 'supername']

Resource[Myns::Sometype, 'supe<caret>rname']{
  name => 42
}

