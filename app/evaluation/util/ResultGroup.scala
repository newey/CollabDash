package evaluation.util

import evaluation.util.ScoreType.ScoreType

/**
 * Created by robert on 27/09/2014.
 */
case class ResultGroup (
                         modelId: Int,
                         scoreTypes: List[ScoreType],
                         scores: List[List[Double]],
                         description: String,
                         testSize: Int,
                         trainSize: Int,
                         lost: Int)
