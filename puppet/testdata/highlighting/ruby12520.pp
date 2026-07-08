class apache-ssl inherits apache {
      Service['apache'] { require +> [ File['apache.pem'], File['/etc/httpd/conf/httpd.conf'] ] }
}