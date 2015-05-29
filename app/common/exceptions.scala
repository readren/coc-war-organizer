package common

/**
 * @author Gustavo
 */
class AlreadyExistException(message:String, cause:Throwable = null) extends Exception(message, cause)

/**Thrown when a player tries to operate on a organization he don't belongs to (almost sure malicious attack) */
class OwnershipFailedException(message:String) extends Exception(message) {
	def this() = this("Possible malicious attack") 
}

/**Thrown when a player tries to do something not allowed to his role (possible malicious attack) */
class NoPrivilegeException(message:String) extends Exception(message)

/**Thrown when a required entity stopped to exist (possible malicious attack)*/
class StoppedToExistException(message:String) extends Exception(message)