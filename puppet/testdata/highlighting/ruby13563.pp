class { 'mysql::server':
config_hash => { 'bind_address' => '0.0.0.0' }
}