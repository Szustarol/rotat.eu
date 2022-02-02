package controllers

import play.api.Logger
import scala.util._
import javax.inject._
import play.api._
import play.api.mvc._
import api._
import play.api.libs.json._
import javax.imageio._
import java.io._
import java.awt.image.BufferedImage
import jobs._


@Singleton
class EnqueueController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  val logger: Logger = Logger(this.getClass())
  def enqueue() = Action (parse.multipartFormData) { request =>
    //Try to load the requested imagez`
    println(request.charset.getOrElse("none"))
    request.body.file("file").flatMap {picture => 
      Try{
        val image = ImageIO.read(picture.ref)
        val requestCode: String = GeneratorData.getNewRequestCode()
        GeneratorData.insertJob(requestCode, image)
        Ok(
          Json.toJson(new UploadAcceptedResponse(requestCode))
        )
      }.toOption
    }.getOrElse(
      BadRequest(
        Json.toJson(
          new ErrorResponse(s"The server was unable to parse the image")
        )
      )
    )
  }
}
