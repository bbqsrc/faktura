import com.natpryce.konfig.ConfigurationProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

fun newDataSource(config: ConfigurationProperties): DataSource {
    var cfg = HikariConfig()

    cfg.dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
    cfg.username = config[database.username]
    cfg.password = config[database.password]

    return HikariDataSource(cfg)
}

fun main(args: Array<String>) {
    val config = ConfigurationProperties.fromResource("config.properties")
    val pool = newDataSource(config)

    // initDb(pool)
    initRoutes(pool)
}