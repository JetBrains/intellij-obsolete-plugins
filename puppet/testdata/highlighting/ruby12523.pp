define php::pear() {
  package { "php-${name}": ensure => installed }
}