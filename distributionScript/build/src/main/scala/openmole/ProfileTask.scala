package openmole

import java.io.File
import fr.ign.cogit.task.profile._

object ProfileScalaTask {
  def apply(folderOut: File, folderIn: File, stepXY: Double, stepZ: Double, maxDist: Double, correlationThreshold: Double, minimalPeriod: Int, heightAttribute: String, dirName: String, numberOfMinimalRepeat: Int, maxPatternLength: Int, maxRepeat: Int): (File) = {
    ProfileTask.runDefault(folderOut, folderIn, stepXY, stepZ, maxDist, correlationThreshold, minimalPeriod, heightAttribute, dirName, numberOfMinimalRepeat, maxPatternLength, maxRepeat);
    (folderOut)
  }

}
