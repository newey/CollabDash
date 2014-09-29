package factories.util

/**
 * Created by robert on 22/09/2014.
 */
class FactoryParameters(params: Array[Param]) {

  def size(): Int = params.size
  def getParam(paramNum: Int): Param = params(paramNum)
  def getParams: Array[Param] = params

  def copy(): FactoryParameters = {
    new FactoryParameters(params.map(_.copy))
  }
}
