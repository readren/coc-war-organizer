package utils

import scala.concurrent.ExecutionContext
import play.libs.Akka

object executionContexts {
  val simpleDbLookups: ExecutionContext = Akka.system.dispatchers.lookup("simple-db-lookups")
  val expensiveDbLookups: ExecutionContext = Akka.system.dispatchers.lookup("expensive-db-lookups")
  val dbWriteOperations: ExecutionContext = Akka.system.dispatchers.lookup("db-write-operations")
  val expensiveCpuOperations: ExecutionContext = Akka.system.dispatchers.lookup("expensive-cpu-operations")
  implicit val default: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}