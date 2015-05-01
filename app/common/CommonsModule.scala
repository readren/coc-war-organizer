package common

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.Play.current

/**
 * The Guice module which wires all dependencies of the commons package.
 */
class CommonsModule extends AbstractModule with ScalaModule {

	/**
	 * Configures the module.
	 */
	def configure() {
		bind[AccountService].to[AccountServiceImpl]
		bind[AccountDao].to[AccountDaoImpl]
	}
}
