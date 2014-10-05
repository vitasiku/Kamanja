package com.ligadata.Serialize

import java.util.Properties
import java.io._
import scala.Enumeration
import scala.io.Source._
import org.apache.log4j._

import com.ligadata.olep.metadata._

import com.twitter.chill.ScalaKryoInstantiator
import com.esotericsoftware.kryo.io.{Input, Output}

case class KryoSerializationException(e: String) extends Throwable(e)

class KryoSerializer extends Serializer{

  lazy val kryoFactory = new ScalaKryoInstantiator

  val loggerName = this.getClass.getName
  lazy val logger = Logger.getLogger(loggerName)

  def SetLoggerLevel(level: Level){
    logger.setLevel(level);
  }


  def SerializeObjectToByteArray(obj : Object) : Array[Byte] = {
    try{
      val kryo = kryoFactory.newKryo()
      val baos = new ByteArrayOutputStream
      val output = new Output(baos)
      kryo.writeClassAndObject(output,obj)
      output.close()
      val ba = baos.toByteArray()
      logger.trace("Serialized data contains " + ba.length + " bytes ")
      ba
    }catch{
      case e:Exception => {
	throw new KryoSerializationException("Failed to Serialize the object(" + obj.getClass().getName() + "): " + e.getMessage())
      }
    }
  }


  def DeserializeObjectFromByteArray(ba: Array[Byte]) : Object = {
    try{
      val kryo = kryoFactory.newKryo()
      val bais = new ByteArrayInputStream(ba);
      val inp = new Input(bais)
      val m = kryo.readClassAndObject(inp)
      logger.trace("DeSerialized object => " + m.getClass().getName())
      inp.close()
      m
    }catch{
      case e:Exception => {
	throw new KryoSerializationException("Failed to DeSerialize the object:" + e.getMessage())
      }
    }
  }

}