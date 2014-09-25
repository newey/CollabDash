package factories

/**
 * Created by robert on 22/09/2014.
 */
trait DataSourceFactory extends FactoryBase{
  def buildDataSource(fp: FactoryParameters, cdp: CollabDashParameters): DataSource
}
