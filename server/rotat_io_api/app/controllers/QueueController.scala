package controllers

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
class QueueController @Inject()(val controllerComponents: ControllerComponents) extends BaseController{
    def checkQueuePosition(accessKey: String) = Action{request => 
        val jobPosition = GeneratorData.getJobQueuePosition(accessKey)
        jobPosition match {
            case Some(value) => Ok(Json.toJson(
                    new QueuePositionResponse(value, accessKey)
            ))
            case None => BadRequest(Json.toJson(
                new ErrorResponse("The requested image does not exist.")
            ))
        }
    }
}