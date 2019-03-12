package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import models.PostEvent
import monix.execution.Scheduler
import services.{PubSubService, PubSubServiceImpl}

/**
  * Binds the PubSubService to its PubSubServiceImpl implementations
  * to use it in the Dependency Injection mechanism.
  */
class PubSubModule extends AbstractModule {

  /** @inheritdoc */
  override def configure(): Unit = {
    bind(classOf[Scheduler]).toInstance(Scheduler.Implicits.global)
  }

  /**
    * Binds PubSubService to its implementation
    *
    * @param scheduler is scala.concurrent.ExecutionContext that additionally can
    *                  schedule the execution of units of work to run with a delay or periodically
    * @return an instance of the PubSubService implementation
    */
  @Provides
  @Singleton
  def pubSubService(implicit scheduler: Scheduler): PubSubService[PostEvent] = {
    new PubSubServiceImpl[PostEvent]
  }
}
