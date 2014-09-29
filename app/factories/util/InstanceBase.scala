package factories.util

import databases.InstanceType.InstanceType

/**
 * Created by robert on 29/09/2014.
 */
abstract class InstanceBase(desc: String, facId: Long, instType: InstanceType) extends Serializable {
  private var computeTime: Long = -1

  def getDescription = desc
  def getFactoryId = facId
  def getInstanceType = instType
  def getComputeTime = computeTime
  def setComputeTime(time: Long) = {computeTime = time}
}
