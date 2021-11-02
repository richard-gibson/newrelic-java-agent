/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{FlowShape, Materializer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source, Unzip, Zip}
import com.newrelic.agent.bridge.{AgentBridge, Token, TransactionNamePriority}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{NewRelic, Trace}
import com.nr.instrumentation.akkahttpcore.{RequestWrapper, ResponseWrapper}

import scala.concurrent.{ExecutionContext, Future}


object FlowRequestHandler {


  def instrumentFlow[Mat](handlerFlow: Flow[HttpRequest, HttpResponse, Any], mat: Materializer)
  : Flow[HttpRequest, HttpResponse, Any] =
    Flow[HttpRequest].mapAsync(1)(new AsyncRequestHandler(toAsyncFunc(handlerFlow)(mat))(mat.executionContext))

  def toAsyncFunc[I, O](flow: Flow[I, O, _])(implicit mat: Materializer) : I => Future[O] =
    i => Source.single(i).via(flow).runWith(Sink.head)

}
