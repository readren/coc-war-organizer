package utils

/**
 * Technology agnostic representation of the ways in which a chunk of stateful/effectful code that accesses a transactional information system affects the information maintained by said system. There are two ways: inside or outside a transaction. When changes are done inside a transaction, they can be undone.
 * The representation should be reliable at least until any of its effectful operations is called. After that this representation may be untruly (and in most implementations it will).
 **/
trait TransacMode {
	/**Tells  */
	def isInside: Boolean
	/**Tells if this instance represents reliably the way in which the effects are treated.
	 * Note this operation is referentially opaque*/
	def isTruly: Boolean
}

/**Represents the "Outside of a transaction" way in which the external effects are treated */
trait OutTransacMode extends TransacMode {
	override def isInside = false
	/**Starts the underneath transaction and gives a new representation of the transactional way which represents it truly.
	 * After this operation is called, this way representation will likely be misleading, because the underneath thing it is attached to might have mutated. */
	def begin: InTransacMode
}

/**Represents the "Inside of a transaction" way in which the external effects are treated */
trait InTransacMode extends TransacMode {
	override def isInside = true
	/**Commits the underneath transaction and gives a new representation of the transactional way which represents it truly.
	 * After this operation is called, this way representation will likely be misleading, because the underneath thing it is attached to might have mutated. */
	def commit: OutTransacMode
	/**Rolls back the underneath transaction and gives a new representation of the transactional way which represents it truly.
	 * After this operation is called, this way representation will likely be misleading, because the underneath thing it is attached to might have mutated. */
	def rollback: OutTransacMode
}


