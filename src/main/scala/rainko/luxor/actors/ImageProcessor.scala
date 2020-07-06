package rainko.luxor.actors

import java.awt.image.BufferedImage

import akka.actor.Actor

case class BrightnessCalculationRequest(image: BufferedImage, rowNumber: Int)

class ImageProcessor extends Actor {
  type Red = Int
  type Green = Int
  type Blue = Int
  type Brightness = Double

  override def receive: Receive = {
    case BrightnessCalculationRequest(image, rowNumber) =>
//      println("HERE")
      val pixels: Seq[(Red, Green, Blue)] = pixelRow(image, rowNumber)
      val summedBrightnessValues = pixels.foldLeft(0.0) { (acc, curr) =>
        val (red, green, blue) = curr
        acc + (red + green + blue) / 3
      }
      val brightness: Brightness = summedBrightnessValues / pixels.size
      context.parent ! BrightnessResponse(brightness)
  }

  private def pixelRow(image: BufferedImage, rowNumber: Int): Seq[(Red, Green, Blue)] = {
    for {
      pixelColumn <- 0 until image.getWidth // potential off by 1
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
