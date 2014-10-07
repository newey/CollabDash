package utilities

import collabfiltermodels.ALSFactory.ALSFactory
import collabfiltermodels.UserSimilarityFactory.UserSimilarityFactory
import collabfiltermodels.guesszero.GuessZeroFactory
import collabfiltermodels.itemaverage.ItemAverageFactory
import collabfiltermodels.itemsim.ItemSimilarityFactory
import datasources.nofilter.NoFilterFactory
import evaluation.TrainTestEvaluator
import factories.{EvaluationFactory, CollabFilterModelFactory, TopicModelFactory, DataSourceFactory}
import topicmodels.empty.EmptyTopicModelFactory
import topicmodels.mahoutLDA.MahoutLDAFactory


object FactoryRegister {
  val dataSourceFactories = List[DataSourceFactory](NoFilterFactory)
  val topicModelFactories = List[TopicModelFactory](EmptyTopicModelFactory, MahoutLDAFactory)
  val collabFilterModelFactories = List[CollabFilterModelFactory](ItemAverageFactory, GuessZeroFactory, ItemSimilarityFactory, UserSimilarityFactory, ALSFactory)
  val evaluationFactories = List[EvaluationFactory](TrainTestEvaluator)
}
