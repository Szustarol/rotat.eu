package controllers

import scala.util._
import javax.inject._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api._
import play.api.mvc._
import api._
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._
import play.api.mvc.AnyContentAsMultipartFormData
import play.api.libs.json._
import javax.imageio._
import play.api.http._
import play.api.libs.Files._
import play.api.http.HttpEntity
import java.io._
import akka.util.ByteString
import java.awt.image.BufferedImage
import jobs._

@Singleton
class ResultController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

    def serveBufferedImage(bufferedImage: BufferedImage) = {
        val bos = new ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", bos)
        ByteString.fromArray(bos.toByteArray())
    }


    def getResult(accessKey: String, rotation: String) = Action{request => 
        val jobResult = GeneratorData.getJobResult(accessKey)


        jobResult match {
            case Some(files) => {
                if (files contains rotation){
                    Result(
                        header = ResponseHeader(200, Map.empty),
                        body = HttpEntity.Strict(serveBufferedImage(files(rotation)), Some("image/png"))
                    )
                }
                else{
                    BadRequest(Json.toJson(
                        new ErrorResponse("Requested rotation was not found on the server.")
                    ))
                }
            }
            case None => BadRequest(Json.toJson(
                new ErrorResponse("The result was not found on the server")
            ))
        }
    }    
}