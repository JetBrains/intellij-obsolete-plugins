class ruby($version, $gems) {
include rvm
Rvm_system_ruby[$version] -> Rvm_gemset['$version@global']
}

class test{
include test2
$var = $::var
}