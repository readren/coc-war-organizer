package auth.controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.Logger

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (implicit val env: Environment[User, JWTAuthenticator])
  extends Silhouette[User, JWTAuthenticator] {

  /**
   * Returns the user.
   *
   * @return The result to display.
   */
  def user = SecuredAction.async { implicit request =>
    Future.successful(Ok(Json.toJson(request.identity)))
  }

  /**
   * Manages the sign out action.
   */
  def signOut = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))
    request.authenticator.discard(Future.successful(Ok))
  }

  /**
   * Provides the desired template.
   *
   * @param template The template to provide.
   * @return The template.
   */
  def view(template: String) = UserAwareAction { implicit request =>
    template match {
      case "home" => Ok(home.html.home())
      case "settings" => Ok(settings.html.settings())
      case "accountChooser" => Ok(settings.account.html.accountChooser())
      case "clanMembership" => Ok(settings.membership.html.membership())
      case "log" => Ok(log.html.log())
      case "joinRequest" => Ok(log.events.joinRequest.html.joinRequest())
      case "joinResponse" => Ok(log.events.joinResponse.html.joinResponse())
      case "joinCancel" => Ok(log.events.joinCancel.html.joinCancel())
      case "abandon" => Ok(log.events.abandon.html.abandon())
      case "centralCommand" => Ok(war.central.html.central())
      case "signUp" => Ok(auth.views.html.signUp())
      case "signIn" => Ok(auth.views.html.signIn())
      case "navigation" => Ok(navigation.html.navigation.render())
      case _ => NotFound
    }
  }
}
