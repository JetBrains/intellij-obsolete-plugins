class Airport {
  static mappedBy = [outgoingFlights: <warning descr="Property 'departureAirport' has incompatible type 'String'">'departureAirport'</warning>,
          <warning descr="Property 'incomingFlights2' does not exist in 'hasMany' property">incomingFlights2</warning>: 'destinationAirport']
  static hasMany = [outgoingFlights: Route,
          incomingFlights: Route]
}

class Route {
  String departureAirport
  Airport destinationAirport
}
