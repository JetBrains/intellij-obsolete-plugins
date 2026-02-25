class Airport {
  static mappedBy = [outgoingFlights: 'depar<caret>tureAirport',
          incomingFlights: 'destinationAirport']
  static hasMany = [outgoingFlights: Route,
          incomingFlights: Route]
}

class Route {
  Airport departureAirport
  Airport destinationAirport
}