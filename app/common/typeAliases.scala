package common

import utils.Transition
import utils.TransacMode
import utils.InTransacMode
import utils.OutTransacMode
import utils.TransitionTry

/**
 * @author Gustavo
 */
object typeAliases {
  type Position = Short
  type Forecast = Short
  /**seconds until war end */
  type Suwe = Int
  type Percentage = Short
  type Stars = Short

  type FightKind = Boolean
  val ATTACK = true
  val DEFENSE = false

  type TiTac[X] = Transition[TransacMode, X]
  type TiOutTac[X] = Transition[OutTransacMode, X]
  type TiInTac[X] = Transition[InTransacMode, X]
  
  type TtTm[X] = TransitionTry[TransacMode, X]
  type TtOutTm[X] = TransitionTry[OutTransacMode, X]
  type TtInTm[X] = TransitionTry[InTransacMode, X]

}