package com.example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing.lineScanner
import akka.stream.alpakka.csv.scaladsl.CsvToMap.toMapAsStrings
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source, Unzip, Zip}
import akka.stream.{ActorMaterializer, Attributes, FanInShape2, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString

class ZipPreferred[A, B] extends GraphStage[FanInShape2[A, B, (A, B)]] {

  override val shape: FanInShape2[A, B, (A, B)] = new FanInShape2("ZipPrefered")

  def out: Outlet[(A, B)] = shape.out
  val in0: Inlet[A] = shape.in0
  val in1: Inlet[B] = shape.in1

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      outer =>

      var in0Item: Option[A] = None

      private def pushAll(): Unit = {
        in0Item.foreach { a =>
            push(out, (a, grab(in1)))
            pull(in1)
          }
      }

      override def preStart(): Unit = {
        pull(in0)
        pull(in1)
      }

      setHandler(in0, new InHandler {
        override def onPush(): Unit = {
          in0Item = Option(grab(in0))
          pull(in0)
        }

        override def onUpstreamFinish(): Unit = {
          if (!isAvailable(in0)) completeStage()
        }
      })

      setHandler(in1, new InHandler {
        override def onPush(): Unit = {
          pushAll()
        }

        override def onUpstreamFinish(): Unit = {
          if (!isAvailable(in1)) completeStage()
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = ()
      })
    }
}

object Test extends App {

  implicit val system = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val flow: Flow[(Int, String), Map[String, String], NotUsed] = {
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unzip = builder.add(Unzip[Int, String])

      val version = unzip.out0.map("version" -> _.toString)

      val data = unzip.out1
        .map(ByteString.apply)
        .via(lineScanner())
        .via(toMapAsStrings())

      val zip = builder.add(
//        new Zip[(String, String), Map[String, String]]
        new ZipPreferred[(String, String),  Map[String, String]]
      )

      version ~> zip.in0
      data ~> zip.in1

      FlowShape(unzip.in, zip.out)
    }).map { case (version, data) => data + version }
  }

  Source.fromIterator(() => Seq(
    (1, "Col1,Col2\n"),
    (1, "Val1,Val2\n"),
    (2, "Col1,Col2\n"),
    (2, "Val3,Val4\n"))
    .toIterator
  ).via(flow).runForeach(println)
}
