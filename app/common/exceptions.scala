package common

import play.api.http.Status

/**
 * @author Gustavo
 */
class AlreadyExistException(message:String, cause:Throwable = null) extends Exception(message, cause) with WithHttpState {
  val stateCode = Status.CONFLICT
}

/**Thrown when a player tries to operate on a organisation he don't belongs to (almost sure malicious attack) */
class OwnershipFailedException(message:String = "You don't belong to that organisation") extends Exception(message) with WithHttpState {
	def this() = this("Possible malicious attack") 
  val stateCode = Status.FORBIDDEN
}

/**Thrown when a player tries to do something not allowed to his role (possible malicious attack) */
class NoPrivilegeException(message:String = "You haven't enough privileges to perform that operation") extends Exception(message) with WithHttpState {
  val stateCode = Status.FORBIDDEN
}

/**Thrown when a required entity stopped to exist (possible malicious attack)*/
class StoppedToExistException(message:String) extends Exception(message) with WithHttpState {
  val stateCode = Status.GONE
}

class NotFoundException(message:String="You are not part of any organisation") extends Exception(message) with WithHttpState {
  val stateCode = Status.NOT_FOUND
}