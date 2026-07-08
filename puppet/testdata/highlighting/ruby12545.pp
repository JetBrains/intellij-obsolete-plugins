define basket($arg) {
    file{'foo':
         ensure  => present,
         content => "$arg",
        }
    }
@basket { 'fruit': arg => 'apple' }
@basket { 'berry': arg => 'watermelon' }

realize( Basket[fruit], Basket[berry] )