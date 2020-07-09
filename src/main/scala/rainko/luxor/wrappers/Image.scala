package rainko.luxor.wrappers

import java.awt.image._
import java.io.File

import javax.imageio.ImageIO

/**
 * Immutable wrapper class for java.awt.image.BufferedImage.
 * @param bufferedImage BufferedImage instance that this class wraps over.
 */
class Image(private val bufferedImage: BufferedImage) {
  type Red = Int
  type Green = Int
  type Blue = Int
  type Brightness = Double

  /**
   * Scala-esque 'getter' for image width.
   *
   * @return width of the wrapped BufferedImage
   */
  def width: Int = bufferedImage.getWidth

  /**
   * Scala-esque 'getter' for image height.
   *
   * @return height of the wrapped BufferedImage
   */
  def height: Int = bufferedImage.getHeight

  /**
   * Write the wrapped BufferedImage into the specified output directory.
   *
   * @param outputDirectoryPath directory where the image should be saved (without the trailing '/')
   * @param filename a name the image file should be given
   * @param format a format the image file should be given (e.g "png", "jpg" etc.)
   */
  def renderToFile(outputDirectoryPath: String, filename: String, format: String): Unit = {
    val imageFile = new File(s"$outputDirectoryPath/$filename.$format")
    ImageIO.write(bufferedImage, format, imageFile)
  }

  /**
   * Pixel color is represented by 32 bits (8 for alpha, 8 for red, 8 for green and 8 for blue).
   *
   * This method completely ignores the alpha bits by always shifting the bits
   * by 8 for red (the leftmost one), by 16 for green (the middle one) and by 24 for blue (the rightmost one).
   *
   * It then shifts the bits to the right and zeroes the leftmost bits
   * to get accurate (in 0 to 255 range inclusively) reading for each color.
   *
   * @param x pixel coordinate on the X axis
   * @param y pixel coordinate on the Y axis
   * @return a tuple of 3 integers in 0-255 range containing
   *         color representations of respectively red, green and blue colors.
   */
  def rgbAt(x: Int, y: Int): (Red, Green, Blue) = {
    val color = bufferedImage.getRGB(x, y)
    val red = (color << 8) >>> 24
    val green = (color << 16) >>> 24
    val blue = (color << 24) >>> 24
    (red, green, blue)
  }

  /**
   * Calculates pixel brightness using the simplified 'perceived brightness' formula:
   * (red * 0.21) + (green * 0.72) + (blue * 0.07)
   *
   * @param x pixel coordinate on the X axis
   * @param y pixel coordinate on the Y axis
   * @return approximated pixel brightness in 0-255 range
   */
  def brightnessAt(x: Int, y: Int): Brightness = {
    val (red, green, blue) = rgbAt(x, y)
    (red * 0.21) + (green * 0.72) + (blue * 0.07)
  }
}

/**
 * Companion object for the rainko.luxor.wrappers.Image class.
 */
object Image {

  /**
   * Convenience constructor for rainko.luxor.wrappers.Image.
   * @param imagePath absolute path of the image to be loaded.
   * @return wrapped and immutable BufferedImage thru rainko.luxor.wrappers.Image
   */
  def apply(imagePath: String): Image = {
    val image = ImageIO.read(new File(imagePath))
    new Image(image)
  }
}

