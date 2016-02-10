package evaluation.util

/**
 * Created by robert on 26/09/2014.
 */
object ScoreType extends Enumeration {
  type ScoreType = Value
  val MAE, RMSE, MeanAbsGuess, GuessMean, GuessVariance = Value
}
