package io.example.account
package crud.postgres

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object Select:

  def all[A](using tableName: String, codec: Codec[A]) =
    sql"""
          SELECT *
          FROM #$tableName
          """.query(codec)

  def many[A](size: Int)(using tableName: String, codec: Codec[A]) =
    sql"""
         SELECT *
         FROM #$tableName
          WHERE id IN (${uuid.list(size)})
          """.query(codec)

  def byOneField[A, B](fieldType: Codec[String])(using names: (String, String), codec: Codec[A]) =
    sql"""
         SELECT *
         FROM #${names._1}
         WHERE #${names._2} ~ $fieldType
       """.query(codec)



