if $lsbdistcodename == "lenny" {
  file { "/usr/local/bin/send-non-empty-mail":
    ensure => file,
    source => "$root/files/send-non-empty-mail.lenny",
    mode => 0755
  }
} else {
  file { "/usr/local/bin/send-non-empty-mail":
    ensure => file,
    source => "$root/files/send-non-empty-mail",
    mode => 0755
  }
}