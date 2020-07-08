package rainko.luxor.wrappers

import java.awt.Rectangle
import java.awt.image._
import java.io.File
import java.util

import javax.imageio.ImageIO

object Image {
  def apply(imagePath: String): Image = {
    val image = ImageIO.read(new File(imagePath))
    new Image(image)
  }
}

class Image(private val bufferedImage: BufferedImage) {
  type Red = Int
  type Green = Int
  type Blue = Int
  type Brightness = Double

  def width: Int = bufferedImage.getWidth
  def height: Int = bufferedImage.getHeight

  def renderToFile(outputDirectoryPath: String, filename: String, format: String): Unit = {
    val imageFile = new File(s"$outputDirectoryPath/$filename.$format")
    ImageIO.write(bufferedImage, format, imageFile)
  }

  def rgbAt(x: Int, y: Int): (Red, Green, Blue) = {
    val color = bufferedImage.getRGB(x, y)
    val red = (color << 8) >>> 24
    val green = (color << 16) >>> 24
    val blue = (color << 24) >>> 24
    (red, green, blue)
  }

  def brightnessAt(x: Int, y: Int): Brightness = {
    val (red, green, blue) = rgbAt(x, y)
    (red * 0.21) + (green * 0.72) + (blue * 0.07)
  }
}
