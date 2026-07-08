
define myclass(
  $var1,
  $var2
){
  notice 'some'
}

function myfunction(
  $var1,
  $var2
){
  notice 'some'

}

define mytype(
  $var1,
  $var2
){
  notice 'some'

}

node 'mynode' {

}

$a = @(END)
  This is indented 2 spaces in the source, but produces
  a result flush left with the initial 'T'
    This line is thus indented 2 spaces.
  | END


$a = @("Verse 8 of The Raven")
  Then this ebony bird beguiling my sad fancy into smiling,
  By the grave and stern decorum of the countenance it wore,
  `Though thy crest $somevar be shorn and shaven, thou,' I said, `art sure no craven.
  Ghastly grim and ancient raven wandering from the nightly shore -
  Tell me what thy lordly name is on the Night's Plutonian shore!'
  Quoth the raven, `Nevermore.'
  | Verse 8 of The Raven


file{
  'firstfile':
    path => 'path',
    before => 'some';
  'otherfile':
    path => 'path',
    before => 'asdfadsf',
    path => path
}