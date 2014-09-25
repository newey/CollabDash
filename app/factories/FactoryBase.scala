package factories

/**
 * Created by robert on 25/09/2014.
 */
trait FactoryBase {
  def name(): String
  def description(): String
  def parameters(): FactoryParameters
}
