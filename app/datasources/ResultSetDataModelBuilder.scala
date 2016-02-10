package datasources

import java.sql.ResultSet
import java.util
import org.apache.mahout.cf.taste.impl.common.FastByIDMap
import org.apache.mahout.cf.taste.impl.model.{GenericPreference, GenericDataModel}
import org.apache.mahout.cf.taste.model.{Preference, DataModel}


object ResultSetDataModelBuilder  {
  def makeDataModel(rs: ResultSet, numUsers: Int): DataModel = {
    val userPreferenceLists = new FastByIDMap[util.Collection[Preference]](numUsers)
    Array.range(0, numUsers).foreach(userPreferenceLists.put(_, new util.ArrayList[Preference]()))

    while (rs.next()) {
      val userNum = rs.getInt(1)
      val itemNum = rs.getInt(2)
      val rating = rs.getFloat(3)
      userPreferenceLists.get(userNum).add(new GenericPreference(userNum, itemNum, rating))
    }

    val userPreferences = GenericDataModel.toDataMap(userPreferenceLists, true)
    new GenericDataModel(userPreferences)
  }
}
