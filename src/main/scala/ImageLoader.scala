import java.awt.image.BufferedImage
import java.io.File

import akka.actor.Actor
import javax.imageio.ImageIO

case class AbsoluteImagePath(path: String)

class ImageLoader extends Actor {
  type Red = Int
  type Green = Int
  type Blue = Int

  override def receive: Receive = {
    case AbsoluteImagePath(path) => {
      val image = loadImage(path)
      println(s"loaded $path")
    }
  }
  private def loadImage(path: String): BufferedImage = ImageIO.read(new File(path))

  private def rgbRow(image: BufferedImage, row: Int): Seq[(Red, Green, Blue)] = {
    val width = image.getWidth
    val model = image.getColorModel
    for {
      rgbPixel <- (width * row) to (width * row + width)
    } yield (model.getRed(rgbPixel), model.getGreen(rgbPixel), model.getBlue(rgbPixel))
  }


}
