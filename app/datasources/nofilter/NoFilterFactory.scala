package datasources.nofilter

import factories._

/**
 * Created by robert on 23/09/2014.
 */
object NoFilterFactory extends DataSourceFactory {
  override def name(): String = "NoFilter"

  override def parameters(): FactoryParameters = new FactoryParameters(params)

  override def description(): String = "Gets all ratings and words"

  override def buildDataSource(fp: FactoryParameters, cdp: CollabDashParameters): DataSource = {
    val jobsAreItems = fp.getParam(0).getBoolValue
    val wordType = fp.getParam(1).getIntValue
    new NoFilterSource(jobsAreItems, wordType).getDataSource
  }

  private val params = Array(
    new Param(
      "Jobs are items",
      "(0) use users as items, (1)Use jobs as items",
      ParamType.Boolean),
    new Param(
      "NoStem, Stem, Composite",
      "(0) unstemmed words, (1) stemmed words, (2) composite words",
      ParamType.Int)
  )
}
