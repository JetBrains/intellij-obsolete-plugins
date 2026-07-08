Puppet::Type.newtype(:my_file_line) do

  ensurable do
    defaultvalues
    defaultto :present
  end

  newparam(:name, :namevar => true) do
    desc 'An arbitrary name used as the identity of the resource.'
  end

  newparam(:ma_super_param) do
    desc 'No desc for the super param'
  end

  newparam(:match) do
    desc 'An optional regular expression to run against existing lines in the file;\n' +
        'if a match is found, we replace that line rather than adding a new line.'
  end

  newparam(:multiple) do
    desc 'An optional value to determine if match can change multiple lines.'
    newvalues(true, false)
  end

  newparam(:after) do
    desc 'An optional value used to specify the line after which we will add any new lines. (Existing lines are added in place)'
  end

  newparam(:line) do
    desc 'The line to be appended to the file located by the path parameter.'
  end

  newparam(:path) do
    desc 'The file Puppet will ensure contains the line specified by the line parameter.'
    validate do |value|
      unless (Puppet.features.posix? and value =~ /^\//) or (Puppet.features.microsoft_windows? and (value =~ /^.:\// or value =~ /^\/\/[^\/]+\/[^\/]+/))
        raise(Puppet::Error, "File paths must be fully qualified, not '#{value}'")
      end
    end
  end

  # Autorequire the file resource if it's being managed
  autorequire(:file) do
    self[:path]
  end

  validate do
    unless self[:line] and self[:path]
      raise(Puppet::Error, "Both line and path are required attributes")
    end

    if (self[:match])
      unless Regexp.new(self[:match]).match(self[:line])
        raise(Puppet::Error, "When providing a 'match' parameter, the value must be a regex that matches against the value of your 'line' parameter")
      end
    end

  end
end
