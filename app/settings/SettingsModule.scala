package settings

import com.google.inject.AbstractModule
import com.google.inject.Provides

import settings.jdbcDaos.JoinRequestDaoImpl
import settings.jdbcDaos.MemberDaoImpl
import settings.jdbcDaos.MembershipDaoImpl
import settings.jdbcDaos.OrganizationDaoImpl
import javax.sql.DataSource
import net.codingwell.scalaguice.ScalaModule
import play.api.Play.current
import play.api.db.DB
import utils.JdbcTransacTransitionExec
import utils.TransacTransitionExec

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
