require './puppet_parser.rb'

printer = StubPrinter.new

FunctionSpecParser.new("function.html").parse_functions { |obj| printer.print_ruby_functions(obj) }
