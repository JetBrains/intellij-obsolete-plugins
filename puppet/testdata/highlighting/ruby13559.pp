class database {

    package {
        'libmysqlclient-dev': ensure => present;
    }

    class { 'mysql::server':
        config_hash => { 'bind_address' => '0.0.0.0' }
    }

    mysql::db { "${db_name}":
        user     => "${db_user}",
        password => "${db_password}",
        host     => '%',
        grant    => ['all']
    }

    database_user { "${db_user}@localhost": password_hash => mysql_password("${db_password}") }
    database_grant { "${db_user}@localhost": privileges => ['all'] }
}