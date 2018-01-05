package com.dag

import java.sql.{Connection, DriverManager}

import com.dag.bo.{Feed, Language}

class DataService(val path: String) {
  // https://earldouglas.com/posts/scala-jdbc.html

  // connect to the database named "mysql" on port 8889 of localhost
  //val url = "jdbc:h2:file:./dbs/news"
  val url = "jdbc:h2:" + path
  //"tcp://localhost:9092/c:/work/scala/news/dbs//news"
  //jdbc:h2:tcp://localhost:9092//home/david/dbs/news
  val driver = "org.h2.Driver"
  val username = "sa"
  val password = ""

  var connection: Connection = _
  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
  } catch {
    case e: Exception => e.printStackTrace
  }
  //connection.close

  def getLanguages: List[Language] = {
    val q = "SELECT id, iso_name, name FROM LANGUAGE"
    val stmt = connection.createStatement
    val rs = stmt.executeQuery(q)

    def _process(acc: List[Language]): List[Language] =
      if (rs.next()) {
        _process(new Language(rs.getInt("id"), rs.getString("name"), rs.getString("iso_name")) :: acc)
      } else {
        stmt.close
        acc
      }

    _process(Nil)
  }

  def getFeeds(l: String): List[Feed] = {

    val q = "SELECT f.id as id, f.url as url, f.last_update as last_update, l.name as language FROM FEED f, LANGUAGE l where f.language_id = l.id and (f.disabled is null or f.disabled = ?) and l.name = ? order by l.id, f.id"
    val stmt = connection.prepareStatement(q)
    stmt.setBoolean(1, false)
    stmt.setString(2, l)
    val rs = stmt.executeQuery()

    def _process(acc: List[Feed]): List[Feed] =
      if (rs.next()) {
        _process(new Feed(rs.getInt("id"), rs.getString("url"), rs.getString("language")) :: acc)
      } else {
        stmt.close
        acc
      }

    _process(Nil)
  }
}
