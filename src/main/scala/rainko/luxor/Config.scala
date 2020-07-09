package rainko.luxor

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

/**
 * Wrapper class for config.json file reads.
 * It is where you configure the input and output directories for the program.
 */
object Config {
  private val jsonConfig: String = Source.fromResource("config.json")
    .getLines
    .reduce(_+_)
  private val parsedJsonConfig: JsValue = Json.parse(jsonConfig)

  val inputFolderPath: String = (parsedJsonConfig \ "inputFolderPath").as[String]
  val outputFolderPath: String = (parsedJsonConfig \ "outputFolderPath").as[String]
}
