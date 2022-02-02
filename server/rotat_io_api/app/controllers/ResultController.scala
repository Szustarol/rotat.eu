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
class ResultController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with DefaultWriteables{

    class ResultForm(seq: Seq[BufferedImage]){
        val (leftRotation, rightRotation, backRotation) = seq.zip(
            Seq("left", "right", "back")
        ).map{ case (bufferedImage: BufferedImage, partName: String) =>
            val bos = new ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "png", bos)
            MultipartFormData.FilePart(
                partName,
                s"${partName}_result.png",
                Some("image/png"),
                ByteString.fromArray(bos.toByteArray())
            ) 
        } match {
            case Seq(l, r, b) => (l, r, b)
        }

        def getResponse() = {
            MultipartFormData(
                dataParts = Map[String, Seq[String]](), 
                files = Seq(leftRotation, rightRotation, backRotation),
                badParts =Seq()
            )
        }
    }

    implicit def writeableOf_MultipartFormDataWithBs(
        codec: Codec,
        contentType: Option[String]
    ):play.api.http.Writeable[MultipartFormData[akka.util.ByteString]] = {
        writeableOf_MultipartFormData(
            codec,
            Writeable[MultipartFormData.FilePart[akka.util.ByteString]](
                (bs: MultipartFormData.FilePart[akka.util.ByteString]) => bs.ref,
                contentType
            )
        )
    }


    def getResult(accessKey: String) = Action{request => 
        val jobResult = GeneratorData.getJobResult(accessKey)


        jobResult match {
            case Some(files) => {
                val rf = new ResultForm(files)
                Ok(
                    rf.getResponse()
                )(
                    writeableOf_MultipartFormDataWithBs(
                        Codec.utf_8,
                        contentType = Some("multipart/form-data")
                    )
                )
            }
            case None => BadRequest(Json.toJson(
                new ErrorResponse("The result was not found on the server")
            ))
        }
    }    
}