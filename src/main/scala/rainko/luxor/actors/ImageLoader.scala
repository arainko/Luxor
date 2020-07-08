package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Stash}
import javax.imageio.ImageIO
import rainko.luxor.wrappers.Image

//TODO: Add config files
//TODO: Add comments
//TODO: Add

case class AbsoluteImagePath(path: String)
case class OutputFolderPath(path: String)

class ImageLoader extends Actor with Stash {
  override def receive: Receive = {
    case AbsoluteImagePath(path) =>
      val image = Image(path)
      val imageProcessors: Seq[(ActorRef, Int)] = for (pixelRowIndex <- 0 until image.height) yield {
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
    imagePath: String,
    image: Image
  ): Receive = {
    case BrightnessResponse(brightness) =>
      if (responses.size < expectedNumberOfResponses-1) {
        context.become(acceptingResponses(responses :+ brightness, expectedNumberOfResponses, imagePath, image))
      } else {
        val avgBrightness = responses.sum / expectedNumberOfResponses
        println(s"${imagePath.split("/").last}: ${(avgBrightness / 2.55).round}")
        context.become(attachMetadataAndOutput(image, imagePath, avgBrightness))
        unstashAll()
      }

    case _ => stash()
  }

  def attachMetadataAndOutput(
    image: Image,
    imagePath: String,
    averageBrightness: Double
  ): Receive = {
    case OutputFolderPath(outputPath) =>
      val imageFilenameWithFormat = imagePath
        .split('/')
        .last
      val imageFormat = imageFilenameWithFormat
        .split('.')
        .last
      val imageFilename = imageFilenameWithFormat
        .split('.')
        .head
      val normalizedBrightness = (averageBrightness / 2.55).round
      val brightnessClassification = if (normalizedBrightness <= 25) "dark" else "bright"
      val imageFilenameWithMetadata = s"${imageFilename}_${brightnessClassification}_$normalizedBrightness"
      image.renderToFile(outputPath, imageFilenameWithMetadata, imageFormat)
      self ! PoisonPill
  }
}
