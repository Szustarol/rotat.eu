package jobs;
import play.api._;
import java.time.Instant
import javax.imageio._
import scala.concurrent._
import java.io._
import java.awt.image.BufferedImage
import jobs._
import scala.concurrent.duration._
import play.api.libs.concurrent.CustomExecutionContext
import akka.actor.ActorSystem
import java.lang._
import play.api.inject._
import javax.inject._
import com.google.inject.AbstractModule

class StartTasksModule extends AbstractModule{
    override def configure() = {
        println("[Tasks module] Inference configuration starting")
        bind(classOf[InferenceTasks]).asEagerSingleton()
    }
}

@Singleton
class InferenceTasks @Inject() (lifecycle: ApplicationLifecycle, actorSystem: ActorSystem)(implicit executionContext: ExecutionContext){
    println("[Tasks module] Starting inference tasks")
    val nJobs = 8
    var tasks: Seq[RotationGenerator] = for{
        i <- 0 until nJobs
    } yield {
        val job = new RotationGenerator()
        new Thread(job).start
        job
    }

    lifecycle.addStopHook{ () => 
        for{job <- tasks}{job.keepWorking = false}
        Future.successful(())
    }



    actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.seconds, interval=1.minute)({ () => 
        println("[Tasks module] Cleaning unused data")
        val nRemoved = GeneratorData.cleanUnused()
        println(s"[Tasks module] Cleaned ${nRemoved} results")
    })
}

class RotationGenerator extends Runnable{
    var keepWorking :Boolean= true

    def run(): Unit = {
        while(keepWorking){
            println("[Rotation Generator] Searching for work to do")

            val (accessKey, sourceImage) = GeneratorData.getNextJob(keepWorking)
            if(!keepWorking)
                return ()

            val result = Seq(sourceImage, sourceImage, sourceImage)

            GeneratorData.insertJobResult(accessKey, result)
            println("[Rotation Generator] Job finished")
        }
    }
}