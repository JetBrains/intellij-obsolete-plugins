# im a class
class foo {
  file { '/tmp/foo' :
    ensure => present,
  }
}

# im a node
node gar, default, 'something' {
}

# im a define
define baz { }

# im a resource
host { 'cow' : }
