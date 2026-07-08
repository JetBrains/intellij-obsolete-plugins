require './puppet_parser.rb'

def die
  $stderr.puts "Error"
  exit
end

version = ARGV[0] || 'latest'

die unless `rm {core_facts,type,metaparameter}.html`

$stderr.puts "Checking out docs for #{version}"
die unless `wget https://docs.puppetlabs.com/facter/latest/core_facts.html 2> /dev/null`
die unless `wget https://docs.puppetlabs.com/references/#{version}/type.html 2> /dev/null`
die unless `wget https://docs.puppetlabs.com/references/#{version}/metaparameter.html 2> /dev/null`

printer = StubPrinter.new

FactsSpecParser.new("core_facts.html").parse_facts { |obj| printer.print_facts(obj) }
ResourceTypeSpecParser.new("type.html").parse_types { |obj| printer.print_resource_definition_stub(obj) }
MetaparametersSpecParser.new("metaparameter.html").parse_params { |obj| obj['name'] = 'name given to the resource instance'; printer.print_metaparams_stub(obj) }

$stderr.puts "Done."
