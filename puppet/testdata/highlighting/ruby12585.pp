file {'puppet':
    require => File[$nginx],
}