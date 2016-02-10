package factories

import databases.InstanceType
import evaluation.util.ScoreType.ScoreType
import factories.util.InstanceBase

/**
 * Created by robert on 27/09/2014.
 */
case class EvaluationGroup (evaluations: List[Evaluation],
                       description: String,
                       testSize: Int,
                       trainSize: Int,
                       dataId: Int) extends InstanceBase(description, 1234, InstanceType.evaluation) {
}

case class Evaluation (modelId: Int, scores: List[Score], evalTime: Long, lost: Int) extends Serializable
case class Score (scoreType: ScoreType, scoreVal: Double) extends Serializable