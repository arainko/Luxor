package rainko.luxor.actors

import java.awt.image.BufferedImage

import akka.actor.{Actor, PoisonPill}

case class BrightnessCalculationRequest(image: BufferedImage, rowNumber: Int)

class ImageProcessor extends Actor {
  type Red = Int
  type Green = Int
  type Blue = Int
  type Brightness = Double

  override def receive: Receive = {
    case BrightnessCalculationRequest(image, rowNumber) =>
      val pixels: Seq[(Red, Green, Blue)] = pixelRow(image, rowNumber)
      val summedBrightnessValues = pixels.foldLeft(0.0) { (acc, curr) =>
        val (red, green, blue) = curr
        acc + (red * 0.21) + (green * 0.72) + (blue * 0.07)
      }
      val brightness: Brightness = summedBrightnessValues / pixels.size
      context.parent ! BrightnessResponse(brightness)
      self ! PoisonPill
  }

  private def pixelRow(image: BufferedImage, rowNumber: Int): Seq[(Red, Green, Blue)] = {
    for {
      pixelColumn <- 0 until image.getWidth
    } yield {
//      println(s"Width: $width, current pixel: $pixelColumn")
      val color = image.getRGB(pixelColumn, rowNumber)
      val red = (color << 8) >>> 24
      val green = (color << 16) >>> 24
      val blue = (color << 24) >>> 24
      (red, green, blue)
    }
  }


}
