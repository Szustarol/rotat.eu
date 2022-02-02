package jobs;

import play.api._;
import java.time.Instant
import javax.imageio._
import java.io._
import java.awt.image.BufferedImage
import jobs._


object GeneratorData{
    private val handledRequests = scala.collection.concurrent.TrieMap[String, Option[Seq[BufferedImage]]]()
    private val jobIndices = scala.collection.concurrent.TrieMap[String, Long]()
    private val cleaningJobs = scala.collection.mutable.ArrayDeque[(Long, String)]()
    private var jobQueue = scala.collection.mutable.ArrayDeque[(String, BufferedImage)]() 
    private val queueLock = new java.util.concurrent.locks.ReentrantLock()
    private val queueCondition = queueLock.newCondition()
    private val outputQueueLock = new java.util.concurrent.locks.ReentrantLock()
    private val outputQueueCondition = outputQueueLock.newCondition()
    private var nextJobIndex: Long = 1;
    private var nJobsDone: Long = 0;
    var cleanDelay: Long = 100

    def cleanUnused(): Long = {
        outputQueueLock.lock()
        var cleaningRemaining: Boolean = true
        var nRemoved: Long = 0
        while(cleaningJobs.length > 0 && cleaningRemaining){
            if(Instant.now.getEpochSecond - cleaningJobs.head._1 > cleanDelay){
                val (_, accessKey) = cleaningJobs.removeHead()
                handledRequests.remove(accessKey)
                jobIndices.remove(accessKey)
                nRemoved += 1
            }
            else{
                cleaningRemaining = false
            }
        }
        outputQueueLock.unlock()
        nRemoved
    }

    def getNewRequestCode(): String = {
        Instant.now.getEpochSecond.toString + nextJobIndex.toString
    }

    def insertJob(accessKey: String, jobImage: BufferedImage): Unit = {
        jobIndices.addOne(accessKey -> nextJobIndex)
        nextJobIndex += 1
        queueLock.lock()
        jobQueue.addOne(accessKey -> jobImage)
        queueCondition.signal()
        queueLock.unlock()
    }

    def insertJobResult(accessKey: String, jobImages: Seq[BufferedImage]): Unit = {
        handledRequests.addOne(accessKey -> Some(jobImages))
        outputQueueLock.lock()
        cleaningJobs.addOne(Instant.now.getEpochSecond -> accessKey)
        outputQueueCondition.signal()
        outputQueueLock.unlock()
    }

    def getNextJob(keepWorking: Boolean): (String, BufferedImage) = {
        queueLock.lock()
        while(jobQueue.length < 1){
            queueCondition.awaitNanos(1000*1000*1000)
            if(!keepWorking)
                return ("", null)
        }
        val job = jobQueue.removeHead()
        handledRequests.addOne(job._1 -> None)
        queueLock.unlock()

        job
    }

    def isJobDone(accessKey: String): Option[Boolean] = {
        if(handledRequests.contains(accessKey) && handledRequests.get(accessKey) != None)
            Some(true)
        else if(handledRequests.contains(accessKey))
            Some(false)
        else if(jobIndices.contains(accessKey))
            Some(false)
        else
            None
    }

    def getJobResult(accessKey: String): Option[Seq[BufferedImage]] = {
        if(isJobDone(accessKey).getOrElse(false)){
            handledRequests.getOrElse(accessKey, None)
        }
        else
            None
    }

    def getJobQueuePosition(accessKey: String): Option[Long] = {
        if (isJobDone(accessKey).getOrElse(false))
            Some(0)
        else if (jobIndices.contains(accessKey))
            Some((jobIndices.getOrElse(accessKey, 0L) - nJobsDone).max(1))
        else
            None
    }
}