package rainko.luxor.actors

import akka.actor.{Actor, ActorRef, Props, Stash, Status}
import rainko.luxor.wrappers.Image
import rainko.luxor.{Brightness, DirectoryPath, ImagePath}

import scala.util.{Failure, Success, Try}

case class AbsoluteImagePath(path: ImagePath)
case class OutputFolderPath(path: DirectoryPath)

/**
 * ImageLoader is responsible for loading in the image using the path it was sent
 * and then dispatching enough ImageProcessors to compute average brightness of all
 * pixel rows to then calculate the image brightness and copy it to the output directory
 * with metadata.
 */
class ImageLoader extends Actor with Stash {
  override def receive: Receive = {
    case AbsoluteImagePath(path) =>
      val image: Image = Image(path)
      val imageProcessors: Seq[(ActorRef, Int)] = for (pixelRowIndex <- 0 until image.height) yield {
        val imageProcessor: ActorRef = context.actorOf(Props[ImageProcessor](), s"processor$pixelRowIndex")
        (imageProcessor, pixelRowIndex)
      }
      imageProcessors.foreach { imageProcessorToPixelRowIndex =>
        val (imageProcessor, pixelRowIndex) = imageProcessorToPixelRowIndex
        imageProcessor ! BrightnessCalculationRequest(image, pixelRowIndex)
      }
      context.become(acceptingResponses(IndexedSeq.empty, image.height-1, path, image))
      unstashAll()

    case _ => stash()
  }

  def acceptingResponses(
    responses: IndexedSeq[Brightness],
    expectedNumberOfResponses: Int,
    imagePath: ImagePath,
    image: Image
  ): Receive = {
    case BrightnessResponse(brightness) =>
      if (responses.size < expectedNumberOfResponses) {
        context.become(acceptingResponses(responses :+ brightness, expectedNumberOfResponses, imagePath, image))
      } else {
        val avgBrightness = responses.sum / expectedNumberOfResponses
        context.become(attachMetadataAndOutput(image, imagePath, avgBrightness))
        unstashAll()
      }

    case _ => stash()
  }

  def attachMetadataAndOutput(
    image: Image,
    imagePath: ImagePath,
    averageBrightness: Brightness
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
      val normalizedBrightness: Long = (averageBrightness / 2.55).round
      val brightnessClassification: String = if (normalizedBrightness <= 25) "dark" else "bright"
      val normalizedDarkness: Long = (normalizedBrightness - 100) * -1
      val imageFilenameWithMetadata: String = s"${imageFilename}_${brightnessClassification}_$normalizedDarkness"
      Try { image.renderToFile(outputPath, imageFilenameWithMetadata, imageFormat) } match {
        case Success(_) =>  context.parent ! Status.Success()
        case Failure(exception) => context.parent ! Status.Failure(exception)
      }
      context.stop(self)
  }
}
