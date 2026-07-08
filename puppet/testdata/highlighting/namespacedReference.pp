define apache::vhost ($port, $docroot, $servername, $vhost_name) {
  Class['apache'] -> Apache::Vhost[$title]
}