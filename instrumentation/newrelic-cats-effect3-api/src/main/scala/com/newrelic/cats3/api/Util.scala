package com.newrelic.cats3.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer, Transaction}

import java.util.concurrent.atomic.AtomicInteger

object Util {
  val RETURN_OPCODE = 176
  def wrapTrace[S, F[_]: Sync](body: F[S]): F[S] =
    Sync[F].delay{
      val tracer = AgentBridge.instrumentation.createScalaTxnTracer()
      println(s"tracer: $tracer")
      tracer
    }.redeemWith(
        _ => body,
        tracer => for {
          txn <- Sync[F].delay(AgentBridge.getAgent.getTransaction(false))
          _ <- setupTokenAndRefCount(txn)
          res <- attachErrorEvent(body, tracer)
          _ <- cleanupTxnAndTokenRefCount(txn)
          _ <- Sync[F].delay{
            if (tracer != null)
              tracer.finish(RETURN_OPCODE, null)
            else
              println("tracer null!!!")
          }
        } yield res
      )

  private def attachErrorEvent[S, F[_]: Sync](body: F[S], tracer: ExitTracer): F[S] =
    body
      .handleErrorWith(throwable => {
      tracer.finish(throwable)
      Sync[F].raiseError(throwable)
    })

  private def setupTokenAndRefCount[F[_]: Sync](txn: Transaction): F[Unit] = Sync[F].delay{
    if (txn != null) {
      AgentBridge.activeToken.set(new AgentBridge.TokenAndRefCount(txn.getToken, AgentBridge.getAgent
                                                                                            .getTracedMethod, new
          AtomicInteger(0)))
    }
  }

  private def cleanupTxnAndTokenRefCount[F[_]: Sync](txn: Transaction): F[Unit] = Sync[F].delay{
    println(s"cleanupTxnAndTokenRefCount $txn")
    if (AgentBridge.activeToken != null) {
      AgentBridge.activeToken.remove()
    }
    if(txn != null) {
      txn.expireAllTokens()
    }
  }
}
