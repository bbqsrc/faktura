import com.natpryce.konfig.ConfigurationProperties
import org.h2.jdbcx.JdbcConnectionPool


fun main(args: Array<String>) {
    val config = ConfigurationProperties.fromResource("config.properties")

    val pool = JdbcConnectionPool.create(
            config[database.uri],
            config[database.username],
            config[database.password]
    )

    // initDb(pool)
    initRoutes(pool)
}