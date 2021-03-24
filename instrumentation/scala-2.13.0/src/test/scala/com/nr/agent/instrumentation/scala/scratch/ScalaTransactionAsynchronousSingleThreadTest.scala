package com.nr.agent.instrumentation.scala.scratch

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class ScalaTransactionAsynchronousSingleThreadTest {

  val singleThread: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  @Test
  def oneTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getFirstNumber.map(firstNumber => firstNumber)(singleThread)

    //Then
    Assert.assertEquals(1, Await.result(result, 2.seconds))
    Assert.assertEquals(1, introspector.getTransactionNames.size)
  }

  @Test
  def twoTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getFirstNumber.flatMap(firstNumber =>
      getSecondNumber.map(secondNumber =>
        firstNumber + secondNumber
      )(singleThread)
    )(singleThread)

    //Then
    Assert.assertEquals(3, Await.result(result, 2.seconds))
    Assert.assertEquals(2, introspector.getTransactionNames.size)
  }

  @Test
  def threeTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getFirstNumber.flatMap(firstNumber =>
      getSecondNumber.flatMap(secondNumber =>
        getThirdNumber.map(thirdNumber =>
          firstNumber + secondNumber + thirdNumber
        )(singleThread)
      )(singleThread)
    )(singleThread)

    //Then
    Assert.assertEquals(6, Await.result(result, 2.seconds))
    Assert.assertEquals(3, introspector.getTransactionNames.size)
  }

  @Trace(dispatcher = true)
  def getFirstNumber: Future[Int] = Future {
    println(s"${Thread.currentThread.getName}: getFirstNumber")
    1
  }(singleThread)

  @Trace(dispatcher = true)
  def getSecondNumber: Future[Int] = Future {
    println(s"${Thread.currentThread.getName}: getSecondNumber")
    2
  }(singleThread)

  @Trace(dispatcher = true)
  def getThirdNumber: Future[Int] = Future {
    println(s"${Thread.currentThread.getName}: getThirdNumber")
    3
  }(singleThread)
}
