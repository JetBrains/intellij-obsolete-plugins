case $facts['os']['name'] {
  'Solaris':           { include role::solaris } # Apply the solaris class
  'RedHat', 'CentOS':  { include role::redhat  } # Apply the redhat class
  /^(Debian|Ubuntu)$/: { include role::debian  } # Apply the debian class
  default:             { include role::generic } # Apply the generic class
}

$rootgroup = $facts['os']['family'] ? {
  'Solaris'          => 'wheel',
  /(Darwin|FreeBSD)/ => 'wheel',
  default            => 'root',
}