package com.nr.agent.instrumentation.scala.scratch

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TraceSegment}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import scala.jdk.CollectionConverters._

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class ScalaTransactionNestedSynchronousTest {

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    val traces = introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)
    val segments = traces.flatMap(trace => getSegments(trace.getInitialTraceSegment))

    Assert.assertEquals(1, result)
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

    Assert.assertEquals(3, result)
    Assert.assertEquals(1, introspector.getTransactionNames.size)
    Assert.assertEquals(1, traces.size)
    Assert.assertEquals(3, segments.size)
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

    Assert.assertEquals(6, result)
    Assert.assertEquals(1, introspector.getTransactionNames.size)
    Assert.assertEquals(1, traces.size)
    Assert.assertEquals(4, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Int = getFirstNumber

  @Trace(dispatcher = true)
  def getTwoResults: Int = getFirstNumber + getSecondNumber

  @Trace(dispatcher = true)
  def getThreeResults: Int = getFirstNumber + getSecondNumber + getThirdNumber

  @Trace(dispatcher = true)
  def getFirstNumber: Int = {
    println(s"${Thread.currentThread.getName}: getFirstNumber")
    1
  }

  @Trace(dispatcher = true)
  def getSecondNumber: Int = {
    println(s"${Thread.currentThread.getName}: getSecondNumber")
    2
  }

  @Trace(dispatcher = true)
  def getThirdNumber: Int = {
    println(s"${Thread.currentThread.getName}: getThirdNumber")
    3
  }

  def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}
