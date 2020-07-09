package rainko.luxor.actors

import akka.actor.{Actor, ActorRef, Props, Stash, Status}
import rainko.luxor.wrappers.Image

import scala.util.{Failure, Success, Try}

//TODO: Add comments

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
      context.become(acceptingResponses(IndexedSeq.empty, image.height-1, path, image))
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
      val normalizedDarkness = (normalizedBrightness - 100) * -1
      val imageFilenameWithMetadata = s"${imageFilename}_${brightnessClassification}_$normalizedDarkness"
      Try { image.renderToFile(outputPath, imageFilenameWithMetadata, imageFormat) } match {
        case Success(_) =>  context.parent ! Status.Success()
        case Failure(exception) => context.parent ! Status.Failure(exception)
      }
      context.stop(self)
  }
}
