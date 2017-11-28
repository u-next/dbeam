/*
 * Copyright 2017 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.dbeam

import java.io.File
import java.sql.Timestamp
import java.util.UUID

import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

object JdbcTestFixtures {
  type recordType = (String, Option[Int], BigDecimal, Float, Double,
    Boolean, Int, Long, Timestamp, Option[Timestamp], Byte, UUID)

  val record1: recordType = ("costa rica caffee", None, BigDecimal("7.20"), (82.5).toFloat,
    (320.7).toDouble, true, 17, 200L, new java.sql.Timestamp(1488300933000L), None, 168.toByte,
    UUID.fromString("123e4567-e89b-12d3-a456-426655440000"))
  val record2: recordType = ("colombian caffee", None, BigDecimal("9.20"), (87.5).toFloat,
    (230.7).toDouble, true, 13, 201L, new java.sql.Timestamp(1488300723000L), None, 133.toByte,
    UUID.fromString("123e4567-e89b-a456-12d3-426655440000"))

  class Coffees(tag: slick.jdbc.H2Profile.api.Tag) extends
    Table[recordType](tag, "COFFEES") {
    def name = column[String]("COF_NAME", O.PrimaryKey)
    def supID = column[Option[Int]]("SUP_ID")
    def price = column[BigDecimal]("PRICE")
    def temperature = column[Float]("TEMPERATURE")
    def size = column[Double]("SIZE")
    def isArabic = column[Boolean]("IS_ARABIC")
    def sales = column[Int]("SALES", O.Default(0))
    def total = column[Long]("TOTAL", O.Default(0))
    def created = column[java.sql.Timestamp]("CREATED")
    def updated = column[Option[java.sql.Timestamp]]("UPDATED")
    def bt = column[Byte]("BT")
    def uid = column[UUID]("UID")
    def * = (name, supID, price, temperature, size,
      isArabic, sales, total, created, updated, bt, uid)
  }

  val coffee = TableQuery[Coffees]

  def createFixtures(db: Database, records: Seq[recordType]): Unit = {
    val dbioSeq = DBIO.seq(
      sqlu"DROP TABLE IF EXISTS COFFEES",
      coffee.schema.create,
      coffee ++= records
    )
    Await.result(db.run(dbioSeq), Duration(10, SECONDS))
    val action = sql"select count(*) from coffees".as[(Int)]
    val f = db.run(action)
    Await.result(f, Duration(10, SECONDS))
  }
}

object AvroToJdbcTestFixtures {
  type recordType = (String, String, String, String, Float, Int, Int, Float,
    String, String, String, String, String, Int, String)

  class CountryTable(tag: slick.jdbc.H2Profile.api.Tag) extends Table[recordType](tag, "country") {
    def code = column[String]("code", O.PrimaryKey)
    def name = column[String]("name")
    def continent = column[String]("continent")
    def region = column[String]("region")
    def surfaceArea = column[Float]("surfacearea")
    def indepYear = column[Int]("indepyear")
    def population = column[Int]("population")
    def lifeExpectancy = column[Float]("lifeexpectancy")
    def gnp = column[String]("gnp")
    def gnpold = column[String]("gnpold")
    def localName = column[String]("localname")
    def governmentForm = column[String]("governmentform")
    def headOfState = column[String]("headofstate")
    def capital = column[Int]("capital")
    def code2 = column[String]("code2")
    def * = (code, name, continent, region, surfaceArea, indepYear, population,
      lifeExpectancy, gnp, gnpold, localName, governmentForm, headOfState, capital, code2)
  }
  val country = TableQuery[CountryTable]

  def initializeEmptyDB(db: Database): Unit = {
    val dbioSeq = DBIO.seq(
      sqlu"DROP TABLE IF EXISTS country",
      country.schema.create
    )
    Await.result(db.run(dbioSeq), Duration(10, SECONDS))
    val action = sql"SELECT count(*) FROM country".as[(Int)]
    val f = db.run(action)
    Await.result(f, Duration(10, SECONDS))
  }

  def readAvroTestData(avroFile: File): Unit = {
    val datumReader = new GenericDatumReader[GenericRecord]
    val dataFileReader = new DataFileReader[GenericRecord](avroFile, datumReader)
    var datum: GenericRecord = null
    while (dataFileReader.hasNext) {
       datum = dataFileReader.next(datum)
    }
  }
}
