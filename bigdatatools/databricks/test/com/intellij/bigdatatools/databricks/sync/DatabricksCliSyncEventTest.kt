package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Guards the parsing of `databricks sync --output json` events against the event schema of the
 * pinned CLI version (`DatabricksCliManager.SUPPORTED_CLI_VERSION`). The samples mirror the
 * serialization in the CLI's `libs/sync/event.go`: `put`/`delete`/`dry_run` are `omitempty`,
 * and every event carries `timestamp` and `seq` fields unknown to [DatabricksCliSyncEvent].
 */
class DatabricksCliSyncEventTest {
  private fun parse(line: String): DatabricksCliSyncEvent = BdtJson.fromJsonToClass(line, DatabricksCliSyncEvent::class.java)

  @Test
  fun `start event maps to in-progress with total file count`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:00.000000+02:00","seq":0,"type":"start","put":["a.py","dir/b.py"],"delete":["c.py"]}""")
    assertEquals(SyncStatus.IN_PROGRESS, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.start", 3), event.getStatusMessage())
  }

  @Test
  fun `start event without changes parses with empty defaults`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:00.000000+02:00","seq":0,"type":"start"}""")
    assertEquals(SyncStatus.IN_PROGRESS, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.start", 0), event.getStatusMessage())
  }

  @Test
  fun `partial progress maps to processing message`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:01.000000+02:00","seq":1,"type":"progress","action":"put","path":"dir/b.py","progress":0.5}""")
    assertEquals(SyncStatus.IN_PROGRESS, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.processing", "dir/b.py"), event.getStatusMessage())
  }

  @Test
  fun `finished upload maps to uploaded message`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:02.000000+02:00","seq":2,"type":"progress","action":"put","path":"dir/b.py","progress":1}""")
    assertEquals(SyncStatus.IN_PROGRESS, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.upload", "dir/b.py"), event.getStatusMessage())
  }

  @Test
  fun `finished deletion maps to deleted message`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:03.000000+02:00","seq":3,"type":"progress","action":"delete","path":"c.py","progress":1}""")
    assertEquals(SyncStatus.IN_PROGRESS, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.delete", "c.py"), event.getStatusMessage())
  }

  @Test
  fun `complete event maps to watching for changes`() {
    val event = parse("""{"timestamp":"2026-08-10T10:00:04.000000+02:00","seq":4,"type":"complete","dry_run":false,"put":["a.py"]}""")
    assertEquals(SyncStatus.WATCHING_FOR_CHANGES, event.getStatus())
    assertEquals(DatabricksBundle.message("sync.status.waiting"), event.getStatusMessage())
  }
}
