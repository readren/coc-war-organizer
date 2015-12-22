package utils

object TransactionalTransition {
  /**A `Transition` whose state type is `TransacMode`.*/
  type TiTac[X] = Transition[TransacMode, X]
  /**A `Transition` whose state type is `OutTransacMode`. If an instance is formed by sthat can affect the TransacMode but strictly starts and ends in the OutTransacMode. The TransacMode can temporally change to InTransacMode in some sub-transition but must go back to OutTransacMode in some posterior sub-transition. */
  type TiOutTac[X] = Transition[OutTransacMode, X]
  /**A Transition that strictly starts and ends in the InTransacMode. The TransacMode can but shouldn't temporally change to OuTransacMode in any sub-transition. */
  type TiInTac[X] = Transition[InTransacMode, X]

  type TtTm[X] = TransitionTry[TransacMode, X]
  type TtOutTm[X] = TransitionTry[OutTransacMode, X]
  type TtInTm[X] = TransitionTry[InTransacMode, X]

}