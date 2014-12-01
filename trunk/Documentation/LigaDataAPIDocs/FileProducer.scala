
package com.ligadata.OutputAdapters

import org.apache.log4j.Logger
import java.io.{ OutputStream, FileOutputStream, File, BufferedWriter, Writer, PrintWriter }
import java.util.zip.GZIPOutputStream
import java.nio.file.{ Paths, Files }
import com.ligadata.OnLEPBase.{ AdapterConfiguration, OutputAdapter, OutputAdapterObj, CountersAdapter }
import com.ligadata.AdaptersConfiguration.FileAdapterConfiguration

object FileProducer extends OutputAdapterObj {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, cntrAdapter: CountersAdapter): OutputAdapter = new FileProducer(inputConfig, cntrAdapter)
}

class FileProducer(val inputConfig: AdapterConfiguration, cntrAdapter: CountersAdapter) extends OutputAdapter {
  private[this] val _lock = new Object()

  private[this] val LOG = Logger.getLogger(getClass);

  private[this] val fc = new FileAdapterConfiguration

  //BUGBUG:: Not Checking whether inputConfig is really FileAdapterConfiguration or not. 

  fc.Typ = inputConfig.Typ
  fc.Name = inputConfig.Name
  fc.className = inputConfig.className
  fc.jarName = inputConfig.jarName
  fc.dependencyJars = inputConfig.dependencyJars

  // For File we expect the format "Type~AdapterName~ClassName~JarName~DependencyJars~CompressionString(GZ/BZ2)~FilesList~PrefixMessage~IgnoreLines~AddTimeStampToMsgFlag"
  if (inputConfig.adapterSpecificTokens.size != 5) {
    val err = "We should find only Type, AdapterName, ClassName, JarName, DependencyJars, CompressionString, FilesList, PrefixMessage, IgnoreLines & AddTimeStampToMsgFlag for File Adapter Config:" + inputConfig.Name
    LOG.error(err)
    throw new Exception(err)
  }

  if (inputConfig.adapterSpecificTokens(0).size > 0)
    fc.CompressionString = inputConfig.adapterSpecificTokens(0)

  fc.Files = inputConfig.adapterSpecificTokens(1).split(",").map(str => str.trim).filter(str => str.size > 0)
  if (inputConfig.adapterSpecificTokens(2).size > 0)
    fc.MessagePrefix = inputConfig.adapterSpecificTokens(2)
  if (inputConfig.adapterSpecificTokens(3).size > 0)
    fc.IgnoreLines = inputConfig.adapterSpecificTokens(3).toInt
  fc.AddTS2MsgFlag = (inputConfig.adapterSpecificTokens(4).compareToIgnoreCase("1") == 0)

  //BUGBUG:: Not validating the values in FileAdapterConfiguration 

  //BUGBUG:: Open file to write the data

  // Taking only first file, if exists
  val sFileName = if (fc.Files.size > 0) fc.Files(0).trim else null

  if (sFileName == null || sFileName.size == 0)
    throw new Exception("First File Name should not be NULL or empty")

  var os: OutputStream = null

  val newLine = "\n".getBytes("UTF8")

  val compString = if (fc.CompressionString == null) null else fc.CompressionString.trim

  if (compString == null || compString.size == 0) {
    os = new FileOutputStream(sFileName);
  } else if (compString.compareToIgnoreCase("gz") == 0) {
    os = new GZIPOutputStream(new FileOutputStream(sFileName))
  } else {
    throw new Exception("Not yet handled other than text & GZ files")
  }

  override def send(message: String, partKey: String): Unit = send(message.getBytes("UTF8"), partKey.getBytes("UTF8"))

  // Locking before we write into file
  override def send(message: Array[Byte], partKey: Array[Byte]): Unit = _lock.synchronized {
    try {
      os.write(message);
      os.write(newLine)
      val key = Category + "/" + fc.Name + "/evtCnt"
      cntrAdapter.addCntr(key, 1)
    } catch {
      case e: Exception => {
        LOG.error("Failed to send :" + e.getMessage)
      }
    }
  }

  override def Shutdown(): Unit = _lock.synchronized {
    if (os != null)
      os.close
  }
}
