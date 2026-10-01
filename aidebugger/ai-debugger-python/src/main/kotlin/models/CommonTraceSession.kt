package com.intellij.aidebugger.python.models

//fun createPythonSession(): TraceSession {
//    val sessionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    val sessionCounters = SessionCountersImpl()
//    val expectedPort = getFreePort()
//
//    val transport = DebuggerSimpleClientTransport(
//        DebuggerClientTransportConfig(
//            host = "localhost",
//            port = expectedPort,
//        ),
//        sessionScope
//    )
//
//    val eventsFlow = transport.incoming
//        .receiveAsFlow()
//        .asSerializableTraceEvents()
//
//    val repository = CommonTraceEventsRepository(
//        sessionScope,
//        eventsFlow,
//        sessionCounters,
//    )
//
//    val sessionWithThreadsVm = TraceEventsSessionVM(
//        eventsRepository = repository,
//        sessionCounters = sessionCounters,
//        coroutineScope = sessionScope
//    )
//
//    val session = TraceSession(
//        sessionScope,
//        repository,
//        sessionWithThreadsVm,
//        sessionCounters,
//        expectedPort
//    )
//
//    return session
//}