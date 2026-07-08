class class {

}

$_x = 1
<warning descr="Capitalized variables are deprecated">$A</warning> = 2
<warning descr="Using qualified variable names where any namespace segment begins with _ is deprecated">$::_x</warning> = 1
<warning descr="Using qualified variable names where any namespace segment begins with _ is deprecated">$a::_x</warning> = 2
<warning descr="Capitalized variables are deprecated">$b::A</warning> = 3

class test($_x = 1) {

}

class { 'apache':
  # Well, I didn't want to write here anything
}