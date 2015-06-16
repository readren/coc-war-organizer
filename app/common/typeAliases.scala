package common

import utils.Transition
import utils.TransacMode

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

	type TiTac[X] = Transition[TransacMode, X]
	
}