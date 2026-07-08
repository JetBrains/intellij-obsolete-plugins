class foo {
    class install {
    }

    class config {
    }

    class service {
    }

    include "foo::install"
    include "foo::config"
    include "foo::service"
}