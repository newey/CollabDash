package factories

/**
 * Created by robert on 22/09/2014.
 */
trait DataSourceFactory {
  def name(): String
  def description(): String
  def parameters(): FactoryParameters
  def buildDataSource(fp: FactoryParameters, cdp: CollabDashParameters): DataSource
  val uuid: Long
}
