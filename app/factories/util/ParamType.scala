package factories.util

/**
 * Created by robert on 22/09/2014.
 */
object ParamType extends Enumeration {
  type ParamType = Value
  val Boolean, Int, Float, String, DataSource, TopicModel, CFModel = Value
}