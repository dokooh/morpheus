/*
 * Copyright (c) 2016-2019 "Neo4j Sweden, AB" [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
// tag::full-example[]
package org.opencypher.morpheus.examples

import org.apache.hadoop.fs.FileSystem
import org.opencypher.morpheus.api.{GraphSources, MorpheusSession}
import org.opencypher.morpheus.util.App
import org.opencypher.okapi.api.graph.Namespace
import org.opencypher.okapi.neo4j.io.MetaLabelSupport
import org.opencypher.okapi.neo4j.io.testing.Neo4jTestUtils._

/**
  * Demonstrates how to initialise a Neo4j PGDS with a pre-written schema.
  *
  * Also shows how to get the schema out of the Neo4j PGDS and store it in a filesystem.
  */
object Neo4jCustomSchemaExample extends App {
  // Create Morpheus session
  implicit val session: MorpheusSession = MorpheusSession.local()

  // Connect to a Neo4j instance and populate it with social network data
  // To run a test instance you may use
  //  ./gradlew :okapi-neo4j-io-testing:neo4jStart
  //  ./gradlew :okapi-neo4j-io-testing:neo4jStop
  val neo4j = connectNeo4j(personNetwork)

  // Initialise schema from serialised file
  // This bypasses the automatic schema computation
  // Useful when the schema has already been calculated and stored in a file
  val schemaPath = getClass.getResource("/schema.json").getPath

  val neoNamespace = Namespace("socialNetwork")
  val pgds = GraphSources.cypher.neo4j(neo4j.config)
  session.registerSource(neoNamespace, pgds)

  // If we didn't already have the schema (first time we connect to a Neo4j db, or the db has changed and we need to update)
  // We can write it to file like this:

  // Get the schema for the graph; MetaLabelSupport.entireGraphName refers to the whole Neo4j database
  val schema = pgds.schema(MetaLabelSupport.entireGraphName).get

  // Convert the schema to a serializable JSON format
  val jsonString = schema.toJson
  // Get a filesystem to store the file on
  val fileSystem = FileSystem.get(session.sparkSession.sparkContext.hadoopConfiguration)
  // Write the file for future reading
  // Commented-out; this file already exists in the beginning of this example
//  fileSystem.writeFile(getClass.getResource("/").getPath + "schema.json", jsonString)

  // run queries!
  session.cypher(
    """
      |FROM GRAPH socialNetwork.graph
      |MATCH (a:Person)
      |WHERE a.age > 15
      |MATCH (a)-[:FRIEND_OF]->(b)
      |RETURN b.name
    """.stripMargin)
    .show

  // Reset Neo4j test instance and close the session and driver
  neo4j.close()

  def personNetwork =
    s"""|CREATE (a:Person { name: 'Alice', age: 10 })
        |CREATE (b:Person { name: 'Bob', age: 20})
        |CREATE (c:Person { name: 'Carol', age: 15})
        |CREATE (a)-[:FRIEND_OF { since: '23/01/1987' }]->(b)
        |CREATE (b)-[:FRIEND_OF { since: '12/12/2009' }]->(c)""".stripMargin
}
// end::full-example[]
