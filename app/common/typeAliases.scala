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


}