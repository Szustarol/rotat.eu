import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
import java.lang.System
import java.io._

object Main{
  def main(args : Array[String]): Unit = {
    val modelInputStream = new FileInputStream("simple_model.h5")
    val model = KerasModelImport.importKerasModelAndWeights(modelInputStream, false)
    println("Model loaded!")
  }
}
