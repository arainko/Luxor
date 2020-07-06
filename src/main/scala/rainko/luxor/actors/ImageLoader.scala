package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, Props, Stash}
import akka.actor.typed.scaladsl.Behaviors
import javax.imageio.ImageIO

case class AbsoluteImagePath(path: String)
case class BrightnessResponse(brightness: Double)

class ImageLoader extends Actor with Stash {


  override def receive: Receive = {
    case AbsoluteImagePath(path) =>
      val image = loadImage(path)
      val imageProcessors = for (pixelRowIndex <- 0 until image.getHeight) yield {
//        println("creating")
        val imageProcessor = context.actorOf(Props[ImageProcessor], s"processor$pixelRowIndex")
        (imageProcessor, pixelRowIndex)
      }
      imageProcessors.foreach { imageProcessorToPixelRowIndex =>
        imageProcessorToPixelRowIndex._1 ! BrightnessCalculationRequest(image, imageProcessorToPixelRowIndex._2)
      }
  }
  private def loadImage(path: String): BufferedImage = ImageIO.read(new File(path))



}
