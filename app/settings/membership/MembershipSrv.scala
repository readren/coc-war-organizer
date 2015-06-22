package settings.membership

import java.util.UUID
import scala.annotation.implicitNotFound
import auth.models.User
import log.OrgaEvent
import log.EventsSource
import log.events.joinRequest.JoinResponseEventDto
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes
import settings.account.Account
import utils.TransacMode
import utils.Transition
import settings.account.Account
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import common.typeAliases._

object Role {
	type Code = Char
	private[this] val codeMap = Map(Leader.code -> Leader, Coleader.code -> Coleader, Veteran.code -> Veteran, Novice.code -> Novice)
	def decode(code: Code) = codeMap(code)
	implicit val jsonWrites: Writes[Role] = Writes[Role](_.toJson)
}
trait Role {
	val canAcceptJoinRequests: Boolean
	val canRejectJoinRequests: Boolean
	val canJoinDirectly: Boolean
	val code: Role.Code
	def toJson = JsString(code.toString())
}

case object Leader extends Role {
	override val canAcceptJoinRequests = true
	override val canRejectJoinRequests = true
	override val canJoinDirectly = true
	override val code = 'L'
}
case object Coleader extends Role {
	override val canAcceptJoinRequests = true
	override val canRejectJoinRequests = true
	override val canJoinDirectly = true
	override val code = 'C'
}
case object Veteran extends Role {
	override val canAcceptJoinRequests = true
	override val canRejectJoinRequests = true
	override val canJoinDirectly = false
	override val code = 'V'
}
case object Novice extends Role {
	override val canAcceptJoinRequests = false
	override val canRejectJoinRequests = false
	override val canJoinDirectly = false
	override val code = 'N'
}

case class Organization(id: Organization.Id, clanName: String, clanTag: String, description: Option[String])
object Organization {
	type Id = Int
	implicit val jsonFormat = Json.writes[Organization]
}

/**
 * Each organization member, present and past, have a single icon which represents him inside the organization. Even after having left it.
 * When an account joins back, he gets the same icon. Icons are owned by the organization, not by the account that holds it.
 */
//case class Icon(organizationId: Organization.Id, tag: Icon.Tag, name: String, role: Role, holder: Account.Id)
object Icon {
	type Tag = Int
}

/**Association between an account and an organization */
//case class Membership(organizationId: Organization.Id, memberTag: Icon.Tag, accountId: Account.Id, requestEventId: Event.Id, responseEventId: Event.Id, accepterMemberTag: Icon.Tag)

case class AbandonEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, memberName: String) extends OrgaEvent {
	def toJson: JsValue = AbandonEventDto.jsonFormat.writes(this)
}
object AbandonEventDto {
	implicit val jsonFormat = Json.writes[AbandonEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("abandon")) }
}

case class RoleChangeEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, affectedIconTag: Icon.Tag, newRole: Role, previousRole: Role, changerIconTag: Icon.Tag) extends OrgaEvent {
	def toJson: JsValue = RoleChangeEventDto.jsonFormat.writes(this)
}
object RoleChangeEventDto {
	implicit val jsonFormat = Json.writes[RoleChangeEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("roleChange")) }
}

trait MembershipSrv extends EventsSource[OrgaEvent] {
	def getMembershipStatusOf(accountId: Account.Id): Transition[TransacMode, MembershipStatusDto]
	/**Gives all the [[Organization]]s that fulfill the received criteria.*/
	def searchOrganizations(criteria: SearchOrganizationsCmd): Transition[TransacMode, Seq[Organization]]
	def sendJoinRequest(userId: User.Id, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, MembershipStatusDto]
	def cancelJoinRequest(accountId: Account.Id, becauseAccepted: Boolean): Transition[TransacMode, MembershipStatusDto]
	def leaveOrganization(accountId: Account.Id): Transition[TransacMode, MembershipStatusDto]
	/**
	 * Creates a new [[Organization]] and stores it into the underlying DB.
	 * @return the created [[Organization]]
	 */
	def createOrganization(userId: User.Id, project: CreateOrganizationCmd): Transition[TransacMode, (Organization, IconDto)]
	def getOrganizationOf(accountId: Account.Id): Transition[TransacMode, Option[Organization.Id]]

  def getOrgaStatus(accountId: Account.Id, cmd: GetOrgaStatusCmd):TiTac[GetOrgaStatusDto]
}

