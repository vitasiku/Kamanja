
package com.ligadata.KamanjaManager

import com.ligadata.KamanjaBase.{ BaseMsg, InputData, DelimitedData, JsonData, XmlData, EnvContext }
import com.ligadata.Utils.Utils
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.xml.XML
import scala.xml.Elem
import org.apache.log4j.Logger

// To filter data coming from Input (Queue/file). This takes N input fields and M output fields. Output fields are subset of Input fields.
// We also have KEY. So that we can start sending the user into separate threads from here.
class TransformMessageData {
  private val LOG = Logger.getLogger(getClass);
  def parseCsvInputData(inputData: String, associatedMsg: String, delimiterString: String): (String, MsgContainerObjAndTransformInfo, InputData) = {
    val dataDelim = if (delimiterString != null && delimiterString.size > 0) delimiterString else ","
    val str_arr = inputData.split(dataDelim, -1)
    val inpData = new DelimitedData(inputData, dataDelim)
    inpData.curPos = 0
    if (associatedMsg != null && associatedMsg.size > 0) {
      val msgType = associatedMsg
      val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
      if (msgInfo == null)
        throw new Exception("Not found Message Type \"" + msgType + "\"")
      if (msgInfo.tranformMsgFlds == null) {
        inpData.tokens = str_arr
        return (msgType, msgInfo, inpData)
      }

      val outputFlds = msgInfo.tranformMsgFlds.outputFlds.map(fld => {
        if (fld < 0) {
          // throw new Exception("Output Field Idx \"" + fld + "\" not found valid")
          ""
        } else {
          str_arr(fld)
        }
      })

      inpData.tokens = outputFlds
      return (msgType, msgInfo, inpData)
    }

    // Did not get any Associated Msg. So, follow regular path
    if (str_arr.size == 0)
      throw new Exception("Not found any fields to get Message Type")
    val msgType = str_arr(0).trim

    val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
    if (msgInfo == null)
      throw new Exception("Not found Message Type \"" + msgType + "\"")

    if (msgInfo.tranformMsgFlds == null) {
      val newArray = new Array[String](str_arr.size - 1)
      Array.copy(str_arr, 1, newArray, 0, str_arr.size - 1)
      inpData.tokens = newArray
      return (msgType, msgInfo, inpData)
    }
    val outputFlds = msgInfo.tranformMsgFlds.outputFlds.map(fld => {
      if (fld < 0) {
        // throw new Exception("Output Field Idx \"" + fld + "\" not found valid")
        ""
      } else {
        str_arr(fld + 1) // Message type is one extra field in message than Message Definition (inputFields)
      }
    })

    return (msgType, msgInfo, inpData)
  }

  def parseJsonInputData(inputData: String, associatedMsg: String): (String, MsgContainerObjAndTransformInfo, InputData) = {
    val json = parse(inputData)
    if (json == null || json.values == null)
      throw new Exception("Invalid JSON data : " + inputData)
    val parsed_json = json.values.asInstanceOf[Map[String, Any]]
    val inpData = new JsonData(inputData)

    if (associatedMsg != null && associatedMsg.size > 0) {
      val msgType = associatedMsg
      val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
      if (msgInfo == null)
        throw new Exception("Not found Message Type \"" + msgType + "\"")

      inpData.root_json = Option(parsed_json)
      inpData.cur_json = Option(parsed_json)
      return (msgType, msgInfo, inpData)
    }

    if (parsed_json.size != 1)
      throw new Exception("Expecting only one message in JSON data : " + inputData)
    val msgTypeAny = parsed_json.head._1
    if (msgTypeAny == null)
      throw new Exception("MessageType not found in JSON data : " + inputData)
    val msgType = msgTypeAny.toString.trim
    val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
    if (msgInfo == null)
      throw new Exception("Not found Message Type \"" + msgType + "\"")
    inpData.root_json = Option(parsed_json)
    inpData.cur_json = Option(parsed_json.head._2)

    return (msgType, msgInfo, inpData)
  }

  def parseXmlInputData(inputData: String, associatedMsg: String): (String, MsgContainerObjAndTransformInfo, InputData) = {
    val xml = XML.loadString(inputData)
    if (xml == null)
      throw new Exception("Invalid XML data : " + inputData)
    val inpData = new XmlData(inputData)
    inpData.root_xml = xml
    inpData.cur_xml = inpData.root_xml

    if (associatedMsg != null && associatedMsg.size > 0) {
      val msgType = associatedMsg
      val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
      if (msgInfo == null)
        throw new Exception("Not found Message Type \"" + msgType + "\"")
      return (msgType, msgInfo, inpData)
    }

    val msgTypeAny = (xml \\ "messagetype")
    if (msgTypeAny == null)
      throw new Exception("MessageType not found in XML data : " + inputData)
    val msgType = msgTypeAny.text.toString.trim
    val msgInfo = KamanjaMetadata.getMessgeInfo(msgType)
    if (msgInfo == null)
      throw new Exception("Not found Message Type \"" + msgType + "\"")

    return (msgType, msgInfo, inpData)
  }

  def parseInputData(inputData: String, msgFormat: String, associatedMsg: String, delimiterString: String): (String, MsgContainerObjAndTransformInfo, InputData) = {
    if (msgFormat.equalsIgnoreCase("csv"))
      return parseCsvInputData(inputData, associatedMsg, delimiterString)
    else if (msgFormat.equalsIgnoreCase("json"))
      return parseJsonInputData(inputData, associatedMsg)
    else if (msgFormat.equalsIgnoreCase("xml"))
      return parseXmlInputData(inputData, associatedMsg)
    else throw new Exception("Invalid input data type")
  }

  def execute(data: Array[Byte], format: String, associatedMsg: String, delimiterString: String, uk: String, uv: String): Array[(String, MsgContainerObjAndTransformInfo, InputData)] = {
    val output = parseInputData(new String(data), format, associatedMsg, delimiterString)
    LOG.debug("Processing uniqueKey:%s, uniqueVal:%s, Datasize:%d".format(uk, uv, data.size))
    Array((output._1, output._2, output._3))
  }
}
