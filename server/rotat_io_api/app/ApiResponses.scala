package api

import play.api.libs.json._
import javax.imageio._
import java.io._
import java.awt.image.BufferedImage
import jobs._

sealed trait ApiResponse{
    def responseType: String
    def responseContent: String
    def requestCode: String
}

object ApiResponse{
    implicit def responseWriter[T <: ApiResponse] = new Writes[T] { 
        def writes(response : T) = Json.obj(
            "responseType" -> response.responseType,
            "responseContent" -> response.responseContent,
            "requestCode" -> response.requestCode
        )
    }
}

case class ErrorResponse(reason: String) extends ApiResponse{
    val responseType = "Error"
    val responseContent = reason
    val requestCode = "None"
}

case class UploadAcceptedResponse(val requestCode: String) extends ApiResponse{
    val responseType = "OK"
    val responseContent = ""
}

case class QueuePositionResponse(val queuePosition: Long, val requestCode : String) extends ApiResponse{
    val responseType = "OK"
    val responseContent = queuePosition.toString
}
