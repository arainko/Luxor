package rainko.luxor.actors

import akka.actor.{Actor, ActorRef, Props, Stash, Status}
import rainko.luxor.wrappers.Image
import rainko.luxor.{Brightness, DirectoryPath, ImagePath}

import scala.util.{Failure, Success, Try}

case class AbsoluteImagePath(path: ImagePath)
case class OutputDirectoryPath(path: DirectoryPath)

/**
 * ImageLoader is responsible for loading in the image using the path it was sent
 * and then dispatching enough ImageProcessors to compute average brightness of all
 * pixel rows to then calculate the image brightness and copy it to the output directory
 * with metadata.
 */
class ImageLoader extends Actor with Stash {

  /**
   * Default actor behavior.
   * Accepts an absolute path of one image and stashes all other messages.
   *
   * It is here that the actor loads in the image and creates
   * worker actors and then dispatches them with work (calculating average
   * brightness of one row of pixels).
   *
   * Once it is done doing that it switches to 'acceptingResponses()' behavior
   * and unstashes all messages.
   * */
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

  /**
   * Accepts BrightnessResponse, which essentially is an average brightness
   * of one row of pixels.
   * Stashes all the other messages.
   *
   * Once it receives enough responses it calculates the average brightness of the image
   * and passes it on along with the image and the image path to the next behavior 'attachMetadataAndOutput()'.
   *
   * @param responses sequence of collected worker actor responses (basically a collection of
   *                  calculated average row brightness)
   * @param expectedNumberOfResponses there should be one response per pixel row of the image,
   *                                  so expectedNumberOfResponses should equal image height
   * @param imagePath not used in this behavior. Kept around as state for the next behavior.
   * @param image not used in this behavior. Kept around as state for the next behavior.
   */
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

  /**
   * Accepts OutputDirectoryPath.
   * Normalizes the brightness (to make it in range of 0 to 100), inverts it to
   * calculate the darkness score of the image and then saves the image with metadata attached
   * to the output directory sent to it in the message.
   *
   * The actor pings its parent about the status of the write to file operation
   * and then stops itself.
   *
   * @param image the image to be saved
   * @param imagePath the OG path of the image, used get the image filename and format.
   * @param averageBrightness the average brightness of the image
   */
  def attachMetadataAndOutput(
    image: Image,
    imagePath: ImagePath,
    averageBrightness: Brightness
  ): Receive = {
    case OutputDirectoryPath(outputPath) =>
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
        case Success(result) =>  context.parent ! Status.Success(result)
        case Failure(exception) => context.parent ! Status.Failure(exception)
      }
      context.stop(self)
  }
}
