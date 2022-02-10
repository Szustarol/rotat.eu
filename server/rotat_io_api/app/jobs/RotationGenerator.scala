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
import java.lang.System
import org.tensorflow.Tensor
import org.tensorflow.SavedModelBundle
import java.awt.Image
import java.awt.image.DataBufferFloat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import org.tensorflow.ndarray.NdArray
import org.tensorflow.ndarray.buffer.DataBuffers


object Model{
    // val modelConfigPath = "../../model/model_config.json"
    // val modelWeightsPath = "../../model/model_weights.h5"
    val fullModelPath = "../../model/best_image_model_bundle"

    System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "8096M")
    System.setProperty("org.bytedeco.javacpp.maxbytes", "8096M")

    val modelBundle = SavedModelBundle.load(fullModelPath, "serve")
    // println(modelBundle.metaGraphDef().getSignatureDefMap().get("serving_default"))
    
    implicit def bufferedImageToTensor(x: BufferedImage): Tensor = {

        val resized = new BufferedImage(64, 128, BufferedImage.TYPE_INT_RGB)
        val g2d = resized.createGraphics()
        g2d.drawImage(x, 0, 0, 64, 128, null)
        g2d.dispose()
        
        val fb = FloatBuffer.allocate(64*128*3)
        var index: Int = 0
        for{
            row <- 0 until 128
            col <- 0 until 64
        }{
            val pixel : Int = resized.getRGB(col, row)

            val red : Int = (pixel >> 16) & 0xff
            val green : Int = (pixel >> 8) & 0xff
            val blue : Int = pixel & 0xff
            val (redn, greenn, bluen) = 
                (red.toFloat/255f, green.toFloat/255f, blue.toFloat/255f)
            fb.put(index, redn)
            index = index + 1
            fb.put(index, greenn)
            index = index + 1
            fb.put(index, bluen)
            index = index + 1
        }

        val shape = Shape.of(1, 128, 64, 3)
        
        val dataBuffer = DataBuffers.of(fb.array(), true, false)

        TFloat32.tensorOf(shape, dataBuffer)
    }

    implicit def tensorToBufferedImage(x: Tensor): BufferedImage = {
        val rawTensor = x.asRawTensor()

        val tensorData = rawTensor.data().asFloats()

        val output = new BufferedImage(64, 128, BufferedImage.TYPE_INT_RGB)

        for{
            col <- 0 until 64
            row <- 0 until 128
        }{
            val elemIndex = row*64*3 + col*3
            val r : Int = (tensorData.getObject(elemIndex)*255f).toInt
            val g : Int = (tensorData.getObject(elemIndex+1)*255f).toInt
            val b : Int = (tensorData.getObject(elemIndex+2)*255f).toInt
            output.setRGB(col, row, (r << 16) | (g << 8) | b)
        }

        rawTensor.close()

        output
    }


    def predict(x: BufferedImage): Map[String, BufferedImage] = {
        val inp = bufferedImageToTensor(x)
        val outputFeed = modelBundle.session()
            .runner()
            .feed("serving_default_input_1:0", inp)
            .fetch("StatefulPartitionedCall:0")
            .fetch("StatefulPartitionedCall:1")
            .fetch("StatefulPartitionedCall:2")
            .run()

        val left = outputFeed.get(0)
        val right = outputFeed.get(1)
        val back = outputFeed.get(2)

        val result = Map(
            "left" -> tensorToBufferedImage(left),
            "right" -> tensorToBufferedImage(right),
            "back" -> tensorToBufferedImage(back)
        )

        left.close()
        right.close()
        back.close()
        inp.close()
        outputFeed.close()

        result
    }

}


class StartTasksModule extends AbstractModule{
    override def configure() = {
        println("[Tasks module] Inference configuration starting")
        Model
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

    //Evaluate model 
    Model

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

            val result = Model.predict(sourceImage)

            GeneratorData.insertJobResult(accessKey, result)
            println("[Rotation Generator] Job finished")
        }
    }
}