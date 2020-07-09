package rainko.luxor.actors

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorSystem, Props, Stash, Status}
import javax.imageio.ImageIO
import play.api.libs.json.Json
import rainko.luxor.Config

import scala.io.Source

case class InputOutputDirectoryPaths(inputPath: String, outputPath: String)

class Supervisor extends Actor with Stash {
  type DirectoryPath = String
  type ImagePath = String

  override def receive: Receive = {
    case InputOutputDirectoryPaths(inputPath, outputPath) =>
      val imagePaths = absoluteImagePaths(inputPath)
      val imageLoaders = for (id <- imagePaths.indices) yield {
        context.actorOf(Props[ImageLoader](), s"loader$id")
      }
      imageLoaders.zip(imagePaths).foreach { loaderToImagePathPair =>
        val (loader, imagePath) = loaderToImagePathPair
        loader ! AbsoluteImagePath(imagePath)
        loader ! OutputFolderPath(outputPath)
      }
      context.become(awaitingShutdown(imageLoaders.size - 1, 0, 0))
      unstashAll()

    case _ => stash()
  }

  def awaitingShutdown(expectedReturns: Int, successCount: Int, failureCount: Int): Receive = {
    case Status.Success(_) =>
      if (successCount + failureCount < expectedReturns) {
        context.become(awaitingShutdown(expectedReturns, successCount + 1, failureCount))
      } else {
        println(s"Done. ${successCount + 1} successful and $failureCount failed.")
        context.system.terminate
      }
    case Status.Failure(ex) => if (successCount + failureCount < expectedReturns) {
      println(s"An error occured: ${ex.getMessage}")
      context.become(awaitingShutdown(expectedReturns, successCount, failureCount + 1))
    } else {
      println(s"Done. $successCount successful and ${failureCount + 1} failed.")
      context.system.terminate
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
  val system = ActorSystem("luxor-system")
  val supervisor = system.actorOf(Props[Supervisor](), "supervisor")
  supervisor ! InputOutputDirectoryPaths(Config.inputFolderPath, Config.outputFolderPath)
}
