package modules

import com.google.inject.{AbstractModule, Scopes}
import repositories.{PostRepository, PostRepositoryImpl}

/**
  * Binds the Post repository to its implementation
  * to use it in the Dependency Injection mechanism.
  */
class PostModule extends AbstractModule {

  /** @inheritdoc */
  override def configure(): Unit = {
    bind(classOf[PostRepository]).to(classOf[PostRepositoryImpl]).in(Scopes.SINGLETON)
  }
}
