package rainko.luxor.actors

import akka.actor.{Actor, PoisonPill}
import rainko.luxor.Brightness
import rainko.luxor.wrappers.Image

case class BrightnessCalculationRequest(image: Image, pixelRowIndex: Int)
case class BrightnessResponse(brightness: Brightness)

/**
 * ImageProcessors calculate average brightness of a pixel row.
 */
class ImageProcessor extends Actor {

  /**
   * Accepts BrightnessCalculationRequests which contains the reference to the image and the row index
   * of the pixel row to be calculated.
   *
   * The actor averages the brightness of the row using Image's brightnessAt(x,y) method
   * and then sends it to its parent actor.
   *
   * Having done that the actor stops itself.
   */
  override def receive: Receive = {
    case BrightnessCalculationRequest(image, rowIndex) =>
      val pixelRowBrightness: Seq[Brightness] = for (pixelColumn <- 0 until image.width) yield {
        image.brightnessAt(pixelColumn, rowIndex)
      }
      val rowBrightnessAverage = pixelRowBrightness.sum / image.width
      context.parent ! BrightnessResponse(rowBrightnessAverage)
      context.stop(self)
  }
}
