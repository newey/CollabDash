package factories

import factories.util.{FactoryParameters, FactoryBase, CollabDashParameters}

/**
 * Created by robert on 25/09/2014.
 */
trait CollabFilterModelFactory extends FactoryBase {
  def buildCollabFilterModel(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel
}
