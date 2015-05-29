package app

import com.google.inject.Guice
import com.mohiva.play.silhouette.api.{ Logger, SecuredSettings }
import play.api.GlobalSettings
import auth.SilhouetteModule
import settings.SettingsModule
import scala.concurrent.Future
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results._

/**
 * The global configuration.
 */
object Global extends GlobalSettings with SecuredSettings with Logger {

  /**
   * The Guice dependencies injector.
   */
  val injector = Guice.createInjector(new SilhouetteModule, new SettingsModule)

  /**
   * Loads the controller classes with the Guice injector,
   * in order to be able to inject dependencies directly into the controller.
   *
   * @param controllerClass The controller class to instantiate.
   * @return The instance of the controller class.
   * @throws Exception if the controller couldn't be instantiated.
   */
  override def getControllerInstance[A](controllerClass: Class[A]) = injector.getInstance(controllerClass)
  
  
  override def onNotAuthenticated(request: RequestHeader, lang: Lang): Option[Future[Result]] =
  	Some(Future.successful(Unauthorized("Not authorized. Try loging in again please.")))
}
