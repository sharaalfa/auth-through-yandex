package io.example.account
package crud.postgres

import skunk.*
import skunk.implicits.*
object Insert:

  def one[A, B](fields: String)(using tableName: String, codec: (Codec[A], Codec[B])) =
    sql"""
         INSERT INTO #$tableName(#$fields)
         VALUES (${codec._1})
         RETURNING*
       """.query(codec._2)


