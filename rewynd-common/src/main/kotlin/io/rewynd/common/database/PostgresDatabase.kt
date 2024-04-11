package io.rewynd.common.database

import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.config.datasource
import org.jetbrains.exposed.sql.Database as Connection

class PostgresDatabase(
    config: DatabaseConfig.PostgresConfig,
    conn: Connection = Connection.connect(config.datasource),
) : SqlDatabase(conn)
