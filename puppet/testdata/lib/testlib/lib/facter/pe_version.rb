
Facter.add("my_pe_version") do
  setcode do
    pe_ver = Facter.value("puppetversion").match(/Puppet Enterprise (\d+\.\d+\.\d+)/)
    pe_ver[1] if pe_ver
  end
end

Facter.add("my_is_pe") do
  setcode do
    if Facter.value(:pe_version).to_s.empty? then
      false
    else
      true
    end
  end
end

Facter.add('my_pe_major_version') do
  confine :is_pe => true
  setcode do
    if pe_version = Facter.value(:pe_version)
      pe_version.to_s.split('.')[0]
    end
  end
end

Facter.add(:my_super_fact) do
end

Facter.add(:my_super_fact2, :timeout => 10) do
end
