import play.api._
import play.api.http._
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results._
import play.api.libs.json._
import api._
import javax.inject.Singleton

@Singleton
class ErrorHandler extends HttpErrorHandler{
    override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = Future.successful(
        BadRequest(
            Json.toJson(new ErrorResponse(s"Invalid API call ${request.method} ${request.path} ${message}"))(ApiResponse.responseWriter)
        ) 
    )

    override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(
        InternalServerError(
            Json.toJson(new ErrorResponse(s"Internal error while executing ${request.path}"))(ApiResponse.responseWriter)
        )
    )

}