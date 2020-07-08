package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import javax.imageio.ImageIO
import play.api.libs.json.Json
import rainko.luxor.Config

import scala.io.Source

case class InputFolderPath(path: String)

class Supervisor extends Actor {
  type DirectoryPath = String
  type ImagePath = String

  override def receive: Receive = {
    case InputFolderPath(path) =>
      val imagePaths = absoluteImagePaths(path)
      val imageLoaders = for (id <- imagePaths.indices) yield {
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
//  println(Config.inputFolderPath)
//  println(Config.outputFolderPath)
  val system = ActorSystem("luxor")
  val supervisor = system.actorOf(Props[Supervisor](), "supervisor")
  supervisor ! InputFolderPath(Config.inputFolderPath)
}
