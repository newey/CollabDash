package databases

/**
 * Created by robert on 25/09/2014.
 */
object InstanceType extends Enumeration {
  type InstanceType = Value
  val dataSource, topicModel, cfModel, evaluation = Value
}
