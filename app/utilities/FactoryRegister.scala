package utilities

import datasources.nofilter.NoFilterFactory


object FactoryRegister {
  val dataSourceFactories = List(NoFilterFactory, NoFilterFactory, NoFilterFactory)
  val topicModelFactories = List()
  val collabFilterModelFactories = List()
}
