package factories.util

import utilities.Timing

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.{Future, future}

/**
 * Created by robert on 25/09/2014.
 */
trait FactoryBase[+A <: InstanceBase] {
  val name: String
  val description: String
  final def parameters(): FactoryParameters = new FactoryParameters(params.map(_.copy))

  final def build(fp: FactoryParameters, cdp: CollabDashParameters): Future[A] = future{
    val (ret: A, micros: Long)= Timing(baseBuild(fp, cdp))
    val blah: InstanceBase = ret
    blah.setComputeTime(micros)
    ret
  }

  protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): A
  protected val params: Array[Param]
}
