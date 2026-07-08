$expression = 3/1 + 4/2

if $vhost_dir == 'UNSET' {
        $real_vhost_dir = $operatingsystem ? {
            /(?i-mx:ubuntu|debian)/        => '/etc/apache2/sites-enabled',
            /(?i-mx:centos|fedora|redhat)/ => '/etc/httpd/conf.d',
        }
    }
    else {
        $real_vhost_dir = $vhost_dir
    }