package com.nr.agent.instrumentation.scala.scratch

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class ScalaTransactionSynchronousTest {

  @Test
  def oneTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val result = firstNumber

    //Then
    Assert.assertEquals(1, result)
    Assert.assertEquals(1, introspector.getTransactionNames.size)
  }

  @Test
  def twoTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val secondNumber = getSecondNumber
    val result = firstNumber + secondNumber

    //Then
    Assert.assertEquals(3, result)
    Assert.assertEquals(2, introspector.getTransactionNames.size)
  }

  @Test
  def threeTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val secondNumber = getSecondNumber
    val thirdNumber = getThirdNumber
    val result = firstNumber + secondNumber + thirdNumber

    //Then
    Assert.assertEquals(6, result)
    Assert.assertEquals(3, introspector.getTransactionNames.size)
  }

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
}
