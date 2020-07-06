package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import javax.imageio.ImageIO


case class InputFolderPath(path: String)

class Supervisor extends Actor {
  type DirectoryPath = String
  type ImagePath = String

  override def receive: Receive = {
    case InputFolderPath(path) =>
      val imagePaths = absoluteImagePaths(path)
      val imageLoaders = for (id <- 0 to imagePaths.size) yield {
        context.actorOf(Props[ImageLoader], s"loader$id")
      }
      imageLoaders.zip(imagePaths).foreach { loaderToImagePathPair =>
          val (loader, imagePath) = loaderToImagePathPair
          loader ! AbsoluteImagePath(imagePath)
        }
  }

  private def absoluteImagePaths(path: DirectoryPath): Seq[ImagePath] = {
    val inputDirectory: File = new File(path)
    inputDirectory.list
      .map { imageFilename => s"$path/$imageFilename" }
  }
}

object Main extends App {
  val system = ActorSystem("luxor")

  val supervisor = system.actorOf(Props[Supervisor], "supervisor")

  supervisor ! InputFolderPath("/home/aleksander/IdeaProjects/Luxor/src/main/resources/in")

//type DirectoryPath = String
//type ImagePath = String
//  type Red = Int
//  type Green = Int
//  type Blue = Int
//  type Brightness = Int
//
//private def absoluteImagePaths(path: DirectoryPath): Seq[ImagePath] = {
//  val inputDirectory: File = new File(path)
//  inputDirectory.list
//    .map { imageFilename => s"$path/$imageFilename" }
//}
//
//  private def loadImage(path: String): BufferedImage = ImageIO.read(new File(path))
//
//  private def pixelRow(image: BufferedImage, rowNumber: Int): Unit = {
//    val width = image.getWidth
//    var counter = 0
//
//    while (counter <= image.getHeight()) {
//
//      for {
//        pixelColumn <- 0 until image.getWidth // potential off by 1
//      } yield {
//        //      println(s"Width: $width, current pixel: $pixelColumn")
//        val color = image.getRGB(pixelColumn, rowNumber)
//        val red = (color << 8) >>> 24
//        val green = (color << 16) >>> 24
//        val blue = (color << 24) >>> 24
//        (red, green, blue)
//      }
//      counter += 1
//      println(counter)
//    }
//  }
//
//  val images = absoluteImagePaths("/home/aleksander/IdeaProjects/Luxor/src/main/resources/in")
//    .map { loadImage }
//
//  images.foreach { pixelRow(_, 10) }

}
