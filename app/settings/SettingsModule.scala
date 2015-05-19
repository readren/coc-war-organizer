package settings

import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.sql.DataSource
import net.codingwell.scalaguice.ScalaModule
import play.api.Play.current
import play.api.db.DB
import utils.JdbcTransacTransitionExec
import utils.TransacTransitionExec
import settings.account.AccountService
import settings.account.AccountDao
import settings.account.AccountDaoImpl
import settings.account.AccountServiceImpl
import settings.membership.MembershipSrv
import settings.membership.OrganizationDao
import settings.membership.MembershipSrvImpl
import settings.membership.MembershipDao
import settings.membership.MemberDao
import settings.membership.JoinRequestDao
import settings.membership.jdbcDaos.MemberDaoImpl
import settings.membership.jdbcDaos.JoinRequestDaoImpl
import settings.membership.jdbcDaos.MembershipDaoImpl
import settings.membership.jdbcDaos.OrganizationDaoImpl

/**
 * The Guice module which wires all dependencies of the commons package.
 */
class SettingsModule extends AbstractModule with ScalaModule {

	/**
	 * Configures the module.
	 */
	def configure() {
		bind[AccountService].to[AccountServiceImpl]
		bind[AccountDao].to[AccountDaoImpl]

		bind[MembershipSrv].to[MembershipSrvImpl]

		bind[JoinRequestDao].to[JoinRequestDaoImpl]
		bind[MemberDao].to[MemberDaoImpl]
		bind[MembershipDao].to[MembershipDaoImpl]
		bind[OrganizationDao].to[OrganizationDaoImpl]
		
		bind[TransacTransitionExec].to[JdbcTransacTransitionExec]

	}
	
  @Provides
  def providesDataSource():DataSource = DB.getDataSource() 

}
