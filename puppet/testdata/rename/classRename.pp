class super::testclass123(
  $param1 = 42,
  $param2 = 69
){}
class super::childclass(
  $param1 = 42,
  $param2 = 69
) inherits super::testclass123 {}
class super::childclass::other(
  $param1 = 42,
  $param2 = 69
) inherits Super::Testclass123 {}

include super::testclass123, Class[super::testclass123], 'super::testclass123'
require super::testclass123, Class[super::testclass123], 'super::testclass123'
contain super::testclass123, Class[super::testclass123], 'super::testclass123'
hiera_include super::testclass123, Class[super::testclass123], 'super::testclass123'

class{
  super::testclass123:
    param1 => 123,
    param2 => 123
  ;
  'super::testclass123':
    param1 => 123,
    param2 => 123
  ;
}

notice $super::testcl<caret>ass123::param2