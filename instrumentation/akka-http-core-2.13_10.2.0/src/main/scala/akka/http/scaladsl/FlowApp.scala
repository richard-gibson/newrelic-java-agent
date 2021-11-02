package akka.http.scaladsl

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FlowApp {

  val f: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)


  def toAsyncFunc[I, O](flow: Flow[I, O, Any])(implicit mat: Materializer) : I => Future[O] =
    i => Source.single(i).via(flow).runWith(Sink.head)

  def instrumentedFlow[I, O](flow: Flow[I, O, Any])(implicit mat: Materializer): Flow[I, O, Any] =
    Flow[I].mapAsync(1)(toAsyncFunc(flow))
}
