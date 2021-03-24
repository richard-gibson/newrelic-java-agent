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
@InstrumentationTestConfig(includePrefixes = Array("none"))
class ScalaSegmentNestedAsynchronousMultipleThreadTest {

  val threadPoolOne: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolTwo: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolThree: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))

    Assert.assertEquals(1, Await.result(result, 2.seconds))
    Assert.assertEquals(1, introspector.getTransactionNames.size)
    Assert.assertEquals(1, traces.size)
    Assert.assertEquals(2, segments.size)
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

    Assert.assertEquals(3, Await.result(result, 2.seconds))
    Assert.assertEquals(1, introspector.getTransactionNames.size)
    Assert.assertEquals(1, traces.size)
    Assert.assertEquals(3, segments.size)
  }

  @Test
  def threeNestedTransactions(): Unit = {
    util.Properties.versionString
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeResults

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))

    Assert.assertEquals(6, Await.result(result, 2.seconds))
    Assert.assertEquals(1, introspector.getTransactionNames.size)
    Assert.assertEquals(1, traces.size)
    Assert.assertEquals(4, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Future[Int] = Future.reduceLeft(Seq(getFirstNumber))(_ + _)(threadPoolOne)

  @Trace(dispatcher = true)
  def getTwoResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber))(_ + _)(threadPoolTwo)

  @Trace(dispatcher = true)
  def getThreeResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber, getThirdNumber))(_ + _)(threadPoolThree)

  @Trace
  def getFirstNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getFirstNumber")
    1
  }(threadPoolOne)

  @Trace
  def getSecondNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getSecondNumber")
    2
  }(threadPoolTwo)

  @Trace
  def getThirdNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getThirdNumber")
    3
  }(threadPoolThree)

  def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}
