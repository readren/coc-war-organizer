/**
 *
 */
package settings

/**
 * @author Gustavo
 *
 */
import play.api.libs.json.Json
import java.util.UUID
import utils.Transition
import utils.TransacMode
import auth.models.User


case class Organization(id: Organization.Id, clanName: String, clanTag: String, description: Option[String])
object Organization {
	type Id = UUID
	implicit val jsonFormat = Json.format[Organization]
}

case class Member(organizationId: Organization.Id, tag: Member.Tag, name: String)
object Member {
	type Tag = Int
}

case class MemberDto(tag: Member.Tag, name: String)
object MemberDto {
	implicit val jsonFormat = Json.format[MemberDto]
}

case class CreateOrganizationCmd(accountTag: Account.Tag, accountName:String, clanName: String, clanTag: String, description: Option[String], memberName:Option[String])
object CreateOrganizationCmd {
	implicit val jsonFormat = Json.format[CreateOrganizationCmd]
}

case class SearchOrganizationsCmd(clanName: Option[String], clanTag: Option[String], description: Option[String])
object SearchOrganizationsCmd {
	implicit val jsonFormat = Json.format[SearchOrganizationsCmd]
}

case class JoinRequest(userId: User.Id, accountTag: Account.Tag, organizationId: Organization.Id, rejectionMsg:Option[String])
//object JoinRequest {
//	implicit val jsonFormat = Json.format[JoinRequest]
//}

case class SendJoinRequestCmd(accountTag: Account.Tag, organizationId: Organization.Id)
object SendJoinRequestCmd {
	implicit val jsonFormat = Json.format[SendJoinRequestCmd]
}

case class Membership(organizationId: Organization.Id, memberTag: Member.Tag, userId: User.Id, accountTag: Account.Tag)

case class MembershipStatusDto(organization: Option[Organization], memberDto: Option[MemberDto], rejectionMsg: Option[String])
object MembershipStatusDto {
	implicit val jsonFormat = Json.format[MembershipStatusDto]
}


trait MembershipSrv {
	def getMembershipStatusOf(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, MembershipStatusDto]
	def searchOrganizations(criteria: SearchOrganizationsCmd): Transition[TransacMode, Seq[Organization]]
	def sendJoinRequest(userId: User.Id, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, MembershipStatusDto]
	def cancelJoinRequest(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, MembershipStatusDto]
	def leaveOrganization(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, MembershipStatusDto]
	def createOrganization(userId: User.Id, project: CreateOrganizationCmd): Transition[TransacMode, Organization]
}

