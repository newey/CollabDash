package datasources.nofilter

import factories._
import factories.util.{ParamType, Param, FactoryParameters, CollabDashParameters}

/**
 * Created by robert on 23/09/2014.
 */
object NoFilterFactory extends DataSourceFactory {
  override val name: String = "NoFilter"
  override val description: String = "Gets all ratings and words"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): DataSource = {
    val jobsAreItems = fp.getParam(0).getBoolValue
    val wordType = fp.getParam(1).getIntValue
    new NoFilterSource(jobsAreItems, wordType).getDataSource
  }

  override protected val params: Array[Param] = Array(
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
