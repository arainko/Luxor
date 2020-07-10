package rainko.luxor.actors

import java.io.File

import akka.actor.{Actor, Props, Stash, Status}
import rainko.luxor.DirectoryPath

case class InputOutputDirectoryPaths(inputPath: DirectoryPath, outputPath: DirectoryPath)

/**
 * Supervisor is responsible for initializing and shutting down the system.
 */
class Supervisor extends Actor with Stash {
  type DirectoryPath = String
  type ImagePath = String

  /**
   * Default actor behavior.
   * Accepts InputOutputDirectoryPaths and stashes all the other messages.
   *
   * Given the input and output directory paths it generates all the image paths in
   * the input folder and forwards them to ImageLoaders while also
   * sending them the output path.
   *
   * Once it is done generating all the workers and sending them image paths
   * it switches into 'awaitingShutdown()' behavior and unstashes all the messages.
   */
  override def receive: Receive = {
    case InputOutputDirectoryPaths(inputPath, outputPath) =>
      val imagePaths = absoluteImagePaths(inputPath)
      val imageLoaders = for (id <- imagePaths.indices) yield {
        context.actorOf(Props[ImageLoader](), s"loader$id")
      }
      imageLoaders.zip(imagePaths).foreach { loaderToImagePathPair =>
        val (loader, imagePath) = loaderToImagePathPair
        loader ! AbsoluteImagePath(imagePath)
        loader ! OutputDirectoryPath(outputPath)
      }
      context.become(awaitingShutdown(imageLoaders.size - 1, 0, 0))
      unstashAll()

    case _ => stash()
  }

  /**
   * If the sum of successes and failures equals the number of expected
   * worker actor status reports then the system can be shut down.
   *
   * @param expectedReturns the number of worker actors expected to report back
   * @param successCount the number of successful file writes to the output directory
   * @param failureCount the number of unsuccessful file writes to the output directory
   */
  def awaitingShutdown(expectedReturns: Int, successCount: Int, failureCount: Int): Receive = {
    case Status.Success(_) =>
      if (successCount + failureCount < expectedReturns) {
        context.become(awaitingShutdown(expectedReturns, successCount + 1, failureCount))
      } else {
        println(s"Done. ${successCount + 1} successful and $failureCount failed.")
        context.system.terminate()
      }
    case Status.Failure(ex) => if (successCount + failureCount < expectedReturns) {
      println(s"An error occured: ${ex.getMessage}")
      context.become(awaitingShutdown(expectedReturns, successCount, failureCount + 1))
    } else {
      println(s"Done. $successCount successful and ${failureCount + 1} failed.")
      context.system.terminate()
    }
  }

  /**
   * A helper method that extracts all file paths from the specified directory.
   *
   * @param path the directory path which is to be scanned for files
   * @return sequence of all file paths from the given directory
   */
  private def absoluteImagePaths(path: DirectoryPath): Seq[ImagePath] = {
    val inputDirectory: File = new File(path)
    inputDirectory.list
      .toIndexedSeq
      .map { imageFilename => s"$path/$imageFilename" }
  }
}
