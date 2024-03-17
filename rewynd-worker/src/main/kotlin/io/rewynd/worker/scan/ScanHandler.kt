package io.rewynd.worker.scan

import io.rewynd.common.cache.queue.ScanJobHandler
import io.rewynd.common.database.Database
import io.rewynd.model.LibraryType

fun mkScanJobHandler(db: Database): ScanJobHandler =
    { context ->
        val lib = context.request
        when (lib.type) {
            LibraryType.Show -> ShowScanner(lib, db).scan()
            LibraryType.Movie -> TODO()
            LibraryType.Image -> TODO()
        }
    }
