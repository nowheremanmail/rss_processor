package com.dag

import java.sql.{Connection, DriverManager}

import com.dag.bo.{Feed, Language}

class DataService(val path: String) {
  val url = "jdbc:h2:" + path
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

  def updateInfo(f: Feed, msg: String) = {
    val stmt = connection.prepareStatement("UPDATE feed SET error = ?, last_update=now(), rowver=rowver+1 where id = ?")
    stmt.setString(1, msg)
    stmt.setInt(2, f.id)
    val res = stmt.executeUpdate()
    stmt.close()
  }

  def update(f: String, msg: String) = {
    val stmt = connection.prepareStatement("UPDATE feed SET error = ?, last_update=now(), rowver=rowver+1 where url = ?")
    stmt.setString(1, msg)
    stmt.setString(2, f)
    val res = stmt.executeUpdate()
    stmt.close()
  }

}
