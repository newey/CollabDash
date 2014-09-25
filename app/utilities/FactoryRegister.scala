package utilities

import datasources.nofilter.NoFilterFactory
import factories.{TopicModelFactory, DataSourceFactory, FactoryBase}
import topicmodels.empty.EmptyTopicModelFactory


object FactoryRegister {
  val dataSourceFactories = List[DataSourceFactory](NoFilterFactory, NoFilterFactory, NoFilterFactory)
  val topicModelFactories = List[TopicModelFactory](EmptyTopicModelFactory)
  val collabFilterModelFactories = List[FactoryBase]()
}
