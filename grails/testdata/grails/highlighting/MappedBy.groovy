class Airport {
  static mappedBy = [outgoingFlights: 'departureAirport',
          incomingFlights: 'destinationAirport']
  static hasMany = [outgoingFlights: Route,
          incomingFlights: Route]
}

class Route {
  Airport departureAirport
  Airport destinationAirport
}
