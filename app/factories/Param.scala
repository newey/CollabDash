package factories

/**
 * Created by robert on 23/09/2014.
 */
class Param (name: String, description: String, kind: ParamType.ParamType) {
  def getName = name
  def getDescription = description
  def getType = kind

  def setValue (value :String) {
    if (kind == ParamType.Boolean) {
      setBoolValue(value != "0")
    } else if (kind == ParamType.Int) {
      setIntValue(Integer.valueOf(value))
    } else if (kind == ParamType.Float) {
      setDoubleValue(java.lang.Double.valueOf(value))
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
  def setBoolValue(value: Boolean): Unit = {
    boolVal = value
  }

  def getStringValue = stringVal
  def getDoubleValue = doubleVal
  def getIntValue = intVal
  def getBoolValue = boolVal

  protected var stringVal = ""
  protected var doubleVal = 0.0
  protected var intVal = 0
  protected var boolVal = false
}
