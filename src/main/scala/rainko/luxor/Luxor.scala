package rainko.luxor

import akka.actor.{ActorSystem, Props}
import rainko.luxor.actors.{InputOutputDirectoryPaths, Supervisor}
import rainko.luxor.config.Config

/**
 * An entry point for the program.
 */
object Luxor extends App {
  val system = ActorSystem("luxor-system")
  val supervisor = system.actorOf(Props[Supervisor](), "supervisor")
  supervisor ! InputOutputDirectoryPaths(Config.inputFolderPath, Config.outputFolderPath)
}
