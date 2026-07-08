module Puppet::Parser::Functions
  newfunction(:testfunction, :type => :rvalue, :doc => <<-EOS
This function will swap the existing case of a string.

*Examples:*

    swapcase("aBcD")

Would result in: "AbCd"
    EOS
  ) do |arguments|

  end
end
