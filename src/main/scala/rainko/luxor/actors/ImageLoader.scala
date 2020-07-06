package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Stash}
import akka.actor.typed.scaladsl.Behaviors
import javax.imageio.ImageIO

//TODO: Add config files
//TODO: Add comments
//TODO: Add

case class AbsoluteImagePath(path: String)
case class BrightnessResponse(brightness: Double)

class ImageLoader extends Actor with Stash {

  override def receive: Receive = {
    case AbsoluteImagePath(path) =>
      val image = loadImage(path)
      val imageProcessors: Seq[(ActorRef, Int)] = for (pixelRowIndex <- 0 until image.getHeight) yield {
        val imageProcessor = context.actorOf(Props[ImageProcessor], s"processor$pixelRowIndex")
        (imageProcessor, pixelRowIndex)
      }
      //TODO: CHANGE THAT
      imageProcessors.foreach { imageProcessorToPixelRowIndex =>
        imageProcessorToPixelRowIndex._1 ! BrightnessCalculationRequest(image, imageProcessorToPixelRowIndex._2)
      }
      context.become(acceptingResponses(Seq.empty, imageProcessors.size, path))
      unstashAll()

    case _ => stash()
  }

  def acceptingResponses(
    responses: Seq[Double],
    expectedNumberOfResponses: Int,
    imagePath: String
  ): Receive = {
    case BrightnessResponse(brightness) =>
//      println(s"expected: $expectedNumberOfResponses, actual: ${responses.size}")
      if (responses.size != expectedNumberOfResponses-1)
        context.become(acceptingResponses(responses :+ brightness, expectedNumberOfResponses, imagePath))
      else {
        val avgBrightness = responses.sum / expectedNumberOfResponses
        println(s"${imagePath.split("/").last}: ${(avgBrightness / 2.55).round}")
        self ! PoisonPill
      }
  }



  private def loadImage(path: String): BufferedImage = ImageIO.read(new File(path))



}
