# something trying to get to another file
$test = $myNewVar
class myChildClass(
  $argument1,
  $argument2 = $test,
  File $otherarg = $test
) inherits myParentClass {

}

myResourceType { 'anotherName':
}

MyResourceType['tryMe'] { }

##############################################
# all the other
##############################################
$mytest = $apache::vhost::port

class bluetooth($ensure=present, $autoupgrade=false) {
  # Validate class parameter inputs. (Fail early and fail hard)

  if ! ($ensure in [ "present", "absent" ]) {
    fail("bluetooth ensure parameter must be absent or present")
  }

  if ! ($autoupgrade in [ true, false ]) {
    fail("bluetooth autoupgrade parameter must be true or false")
  }

  # Set local variables based on the desired state

  if $ensure == "present" {
    $service_enable = true
    $service_ensure = running
    if $autoupgrade == true {
      $package_ensure = latest
    } else {
      $package_ensure = present
    }
  } else {
    $service_enable = false
    $service_ensure = stopped
    $package_ensure = absent
  }

  # Declare resources without any relationships in this section

  package { [ "bluez-libs", "bluez-utils"]:
    ensure => $package_ensure,
  }

  service { hidd:
    enable         => $service_enable,
    ensure         => $service_ensure,
    status         => "source /etc/init.d/functions; status hidd",
    hasstatus      => true,
    hasrestart     => true,
  }

  # Finally, declare relations based on desired behavior

  if $ensure == "present" {
    Package["bluez-libs"]  -> Package["bluez-utils"]
    Package["bluez-libs"]  ~> Service[hidd]
    Package["bluez-utils"] ~> Service[hidd]
  } else {
    Service["hidd"]        -> Package["bluez-utils"]
    Package["bluez-utils"] -> Package["bluez-libs"]
  }
}

class myservice($ensure='running') {

  if $ensure+1 in [ running, stopped ] {
    $_ensure = $ensure
  } else {
    fail('ensure parameter must be running or stopped')
  }

  case $::operatingsystem {
    centos: {
      $package_list = 'openssh-server'
    }
    solaris: {
      $package_list = [ SUNWsshr, SUNWsshu ]
    }
    default: {
      fail("Module ${module_name} does not support ${::operatingsystem}")
    }
  }

  $sdlkjhsd = 'something'

  Package { ensure => present, }

  File { owner => '0', group => '0', mode => '0644' }

  package { $package_list: }



  file { "/tmp/${variable}":
    ensure => present,
  }

  service {'myservice':
    ensure    => $_ensure,
    hasstatus => true,
  }
}

$myvar = 0

class ntp(
  $myvar = $ntp::params::server
) inherits ntp::params {

  notify { 'ntp':
    message   => "server=[${myvar}]",
    test      => "${1+1 == 2}",
    more_test => "sdflkjs${
      $myvar == "sdf${
        $myvar + "$myvar$myvar${myvar}"
      }sdf"
    }"
  }

}

$testvar = 0

class ntp_child inherits ntp {

  $myvar = 4
  class sybclass {
    $testvar=3

  }
  $testvar = 1
}

node 'mynode'{
  $myvar = 123
}