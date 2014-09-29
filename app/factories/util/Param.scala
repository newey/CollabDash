package factories.util

/**
 * Created by robert on 23/09/2014.
 */
class Param (name: String, description: String, kind: ParamType.ParamType) {
  def getName = name
  def getDescription = description
  def getType = kind

  def copy: Param = {
    val p = new Param(name, description, kind)
    p.stringVal = stringVal
    p.doubleVal = doubleVal
    p.intVal = intVal
    p.boolVal = boolVal
    p.intArrayVal = intArrayVal
    p
  }

  def setValue (value :String) {
    if (kind == ParamType.Boolean) {
      setBoolValue(value != "0")
    } else if (kind == ParamType.Int || kind == ParamType.DataSource || kind == ParamType.CFModel) {
      setIntValue(Integer.valueOf(value))
    } else if (kind == ParamType.Float) {
      setDoubleValue(java.lang.Double.valueOf(value))
    } else if (kind == ParamType.CFModelList) {
      setIntArrayValue(value.split(" ").map(_.toInt))
    } else {
      setStringValue(stringVal)
    }
  }

  def setStringValue(value: String): Unit = {
    stringVal = value
  }
  def setDoubleValue(value: Double): Unit = {
    doubleVal = value
  }
  def setIntValue(value: Int): Unit = {
    intVal = value
  }
  def setBoolValue(value: Boolean) = {
    boolVal = value
  }
  def setIntArrayValue(value: Array[Int]) {
    intArrayVal = value
  }

  def getStringValue = stringVal
  def getDoubleValue = doubleVal
  def getIntValue = intVal
  def getBoolValue = boolVal
  def getIntArrayValue = intArrayVal

  protected var stringVal = ""
  protected var doubleVal = 0.0
  protected var intVal = 0
  protected var boolVal = false
  protected var intArrayVal = new Array[Int](1)
}
