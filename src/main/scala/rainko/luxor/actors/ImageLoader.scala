package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Stash}
import javax.imageio.ImageIO

//TODO: Add config files
//TODO: Add comments
//TODO: Add

case class AbsoluteImagePath(path: String)
case class OutputFolderPath(path: String)

class ImageLoader extends Actor with Stash {
  override def receive: Receive = {
    case AbsoluteImagePath(path) =>
      val image = loadImage(path)
      val imageProcessors: Seq[(ActorRef, Int)] = for (pixelRowIndex <- 0 until image.getHeight) yield {
        val imageProcessor = context.actorOf(Props[ImageProcessor](), s"processor$pixelRowIndex")
        (imageProcessor, pixelRowIndex)
      }
      imageProcessors.foreach { imageProcessorToPixelRowIndex =>
        val (imageProcessor, pixelRowIndex) = imageProcessorToPixelRowIndex
        imageProcessor ! BrightnessCalculationRequest(image, pixelRowIndex)
      }
      context.become(acceptingResponses(IndexedSeq.empty, imageProcessors.size, path, image))
      unstashAll()

    case _ => stash()
  }

  def acceptingResponses(
    responses: IndexedSeq[Double],
    expectedNumberOfResponses: Int,
    imageFilename: String,
    image: BufferedImage
  ): Receive = {
    case BrightnessResponse(brightness) =>
      if (responses.size < expectedNumberOfResponses) {
        context.become(acceptingResponses(responses :+ brightness, expectedNumberOfResponses, imageFilename, image))
      } else {
        val avgBrightness = responses.sum / expectedNumberOfResponses
        println(s"${imageFilename.split("/").last}: ${(avgBrightness / 2.55).round}")
        context.become(attachMetadataAndOutput(image, imageFilename, avgBrightness))
        unstashAll()
      }

    case _ => stash()
  }

  def attachMetadataAndOutput(
    image: BufferedImage,
    imageFilename: String,
    averageBrightness: Double
  ): Receive = {
    ???
  }

  private def loadImage(path: String): BufferedImage = ImageIO.read(new File(path))



}
