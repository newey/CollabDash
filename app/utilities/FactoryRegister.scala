package utilities

import collabfiltermodels.itemaverage.ItemAverageFactory
import datasources.nofilter.NoFilterFactory
import evaluation.TrainTestEvaluator
import factories.{EvaluationFactory, CollabFilterModelFactory, TopicModelFactory, DataSourceFactory}
import topicmodels.empty.EmptyTopicModelFactory


object FactoryRegister {
  val dataSourceFactories = List[DataSourceFactory](NoFilterFactory, NoFilterFactory, NoFilterFactory)
  val topicModelFactories = List[TopicModelFactory](EmptyTopicModelFactory)
  val collabFilterModelFactories = List[CollabFilterModelFactory](ItemAverageFactory)
  val evaluationFactories = List[EvaluationFactory](TrainTestEvaluator)
}
