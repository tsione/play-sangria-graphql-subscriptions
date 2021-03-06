package modules

import com.google.inject.{AbstractModule, Provides}
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.{H2Profile, JdbcBackend}

/**
  * A trait that declares an interface for the database access object
  * to be injected by the Dependency Injection mechanism.
  */
private[modules] trait Database {
  def config: DatabaseConfig[H2Profile]

  def db: JdbcBackend#DatabaseDef

  def profile: H2Profile
}

trait AppDatabase extends Database

class DBModule extends AbstractModule {

  override def configure(): Unit = ()

  /**
    * Provides a database access object.
    *
    * @param dbConfigProvider provides db configs for the evolutions.default db
    */
  @Provides
  def provideDatabase (dbConfigProvider: DatabaseConfigProvider): AppDatabase = new AppDatabase {
    override val config = dbConfigProvider.get[H2Profile]

    override def db = config.db

    override def profile = config.profile
  }
}
