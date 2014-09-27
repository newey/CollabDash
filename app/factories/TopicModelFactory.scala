package factories

import factories.util.{FactoryParameters, FactoryBase, CollabDashParameters}

/**
 * Created by robert on 25/09/2014.
 */
trait TopicModelFactory extends FactoryBase {
  def buildTopicModel(fp: FactoryParameters, cdp: CollabDashParameters): TopicModel
}
