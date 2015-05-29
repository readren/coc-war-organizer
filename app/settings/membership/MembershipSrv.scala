package settings.membership

import java.util.UUID
import scala.annotation.implicitNotFound
import auth.models.User
import log.Event
import log.EventsSource
import log.events.joinRequest.JoinResponseEventDto
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes
import settings.account.AccountId
import utils.TransacMode
import utils.Transition
import settings.account.Account
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject

trait Role {
	val canAcceptJoinRequests: Boolean
	val canRejectJoinRequests: Boolean
	val canJoinDirectly: Boolean
	val code: Char
	def toJson = JsString(code.toString())
}
object Role {
	private[this] val codeMap = Map(Leader.code -> Leader, Coleader.code -> Coleader, Veteran.code -> Veteran, Novice.code -> Novice)
	def decode(code: Char) = codeMap(code)
	implicit val jsonWrites: Writes[Role] = Writes[Role](_.toJson)
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
	type Id = UUID
	implicit val jsonFormat = Json.writes[Organization]
}

case class Member(organizationId: Organization.Id, tag: Member.Tag, name: String, role: Role, holder: Option[AccountId])
object Member {
	type Tag = Int
}

case class Membership(organizationId: Organization.Id, memberTag: Member.Tag, accountId: AccountId, requestEventId: Event.Id, responseEventId: Event.Id, accepterMemberTag:Member.Tag)

case class AbandonEvent(abandonEventId: Event.Id, accountId: AccountId)

case class AbandonEventDto(id: Event.Id, instant: Event.Instant, memberName: String) extends Event {
	def toJson: JsValue = AbandonEventDto.jsonFormat.writes(this)
}
object AbandonEventDto {
	implicit val jsonFormat = Json.writes[AbandonEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("abandon")) }
}

trait MembershipSrv extends EventsSource[Event] {
	def getMembershipStatusOf(accountId: AccountId): Transition[TransacMode, MembershipStatusDto]
	/**Gives all the [[Organization]]s that fulfill the received criteria.*/
	def searchOrganizations(criteria: SearchOrganizationsCmd): Transition[TransacMode, Seq[Organization]]
	def sendJoinRequest(userId: User.Id, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, MembershipStatusDto]
	def cancelJoinRequest(accountId: AccountId): Transition[TransacMode, MembershipStatusDto]
	def leaveOrganization(accountId: AccountId): Transition[TransacMode, MembershipStatusDto]
	/**
	 * Creates a new [[Organization]] and stores it into the underlying DB.
	 * @return the created [[Organization]]
	 */
	def createOrganization(userId: User.Id, project: CreateOrganizationCmd): Transition[TransacMode, (Organization, Member, Membership)]
	def getOrganizationOf(accountId: AccountId): Transition[TransacMode, Option[Organization.Id]]
}

