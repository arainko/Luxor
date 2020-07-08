package rainko.luxor.actors

import akka.actor.{Actor, PoisonPill}
import rainko.luxor.wrappers.Image

case class BrightnessCalculationRequest(image: Image, pixelRowIndex: Int)
case class BrightnessResponse(brightness: Double)

class ImageProcessor extends Actor {
  type Brightness = Double

  override def receive: Receive = {
    case BrightnessCalculationRequest(image, rowIndex) =>
      val pixelRowBrightness: Seq[Brightness] = for (pixelColumn <- 0 until image.width) yield {
        image.brightnessAt(pixelColumn, rowIndex)
      }
      val rowBrightnessAverage = pixelRowBrightness.sum / image.width
      context.parent ! BrightnessResponse(rowBrightnessAverage)
      self ! PoisonPill
  }
}
