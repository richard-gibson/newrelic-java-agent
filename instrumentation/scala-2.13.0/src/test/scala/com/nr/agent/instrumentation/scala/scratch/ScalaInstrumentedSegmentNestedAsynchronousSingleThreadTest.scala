package com.nr.agent.instrumentation.scala.scratch

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TraceSegment}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl"))
class ScalaInstrumentedSegmentNestedAsynchronousSingleThreadTest {

  val singleThread: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))
    Await.result(finishedTransactionCollection(introspector), 10.seconds)

    Assert.assertEquals("Result", 1, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Test
  def twoNestedTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getTwoResults

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))
    Await.result(finishedTransactionCollection(introspector), 10.seconds)

    Assert.assertEquals("Result", 3, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Test
  def threeNestedTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeResults

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))
    Await.result(finishedTransactionCollection(introspector, 2), 10.seconds)

    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Future[Int] = Future.reduceLeft(Seq(getFirstNumber))(_ + _)(singleThread)

  @Trace(dispatcher = true)
  def getTwoResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber))(_ + _)(singleThread)

  @Trace(dispatcher = true)
  def getThreeResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber, getThirdNumber))(_ + _)(singleThread)

  @Trace
  def getFirstNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getFirstNumber")
    1
  }(singleThread)

  @Trace
  def getSecondNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getSecondNumber")
    2
  }(singleThread)

  @Trace
  def getThirdNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getThirdNumber")
    3
  }(singleThread)

  def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }

  def finishedTransactionCollection(introspector :Introspector, expectedTransactions: Int = 1): Future[Unit] = {
    Thread.sleep(100)
    Future {
      while (introspector.getFinishedTransactionCount() < expectedTransactions) {
        Thread.sleep(100)
      }
    }(singleThread)
  }
}
