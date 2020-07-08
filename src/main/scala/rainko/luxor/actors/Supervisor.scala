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
        context.actorOf(Props[ImageLoader](), s"loader$id")
      }
      imageLoaders.zip(imagePaths).foreach { loaderToImagePathPair =>
          val (loader, imagePath) = loaderToImagePathPair
          loader ! AbsoluteImagePath(imagePath)
          loader ! OutputFolderPath("/home/aleksander/IdeaProjects/Luxor/src/main/resources/out")
        }
  }

  private def absoluteImagePaths(path: DirectoryPath): Seq[ImagePath] = {
    val inputDirectory: File = new File(path)
    inputDirectory.list
      .toIndexedSeq
      .map { imageFilename => s"$path/$imageFilename" }
  }
}

object Main extends App {
  val system = ActorSystem("luxor")

  val supervisor = system.actorOf(Props[Supervisor](), "supervisor")

  supervisor ! InputFolderPath("/home/aleksander/IdeaProjects/Luxor/src/main/resources/in")
}
