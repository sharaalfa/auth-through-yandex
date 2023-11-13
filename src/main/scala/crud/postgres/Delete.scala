package io.example.account
package crud.postgres

import domain.*

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object Delete:

  def many(size: Int)(using tableName: String): Command[List[UUID]] =
    sql"""
          DELETE
            FROM #$tableName
           WHERE id IN (${uuid.list(size)})
       """.command


