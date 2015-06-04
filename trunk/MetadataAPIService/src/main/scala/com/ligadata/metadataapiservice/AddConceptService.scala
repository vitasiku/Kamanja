package com.ligadata.metadataapiservice

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.io.IO

import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import com.ligadata.fatafat.metadata._

import scala.util.{ Success, Failure }

import com.ligadata.MetadataAPI._

object AddConceptService {
	case class Process(conceptJson:String, formatType:String)
}

class AddConceptService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

	import AddConceptService._
	
	implicit val system = context.system
	import system.dispatcher
	val log = Logging(system, getClass)
  val APIName = "AddConceptService"
	
	def receive = {
	  case Process(conceptJson, formatType) =>
	    process(conceptJson, formatType)
	    context.stop(self)
	}
	
	def process(conceptJson:String, formatType:String): Unit = {
	  log.debug("Requesting AddConcept {},{}",conceptJson,formatType)
    
    var nameVal: String = null
    if (formatType.equalsIgnoreCase("json")) {
      nameVal = APIService.extractNameFromJson(conceptJson,AuditConstants.CONCEPT) 
    } else {
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:Unsupported format: "+formatType).toString ) 
      return
    }

	  if (!MetadataAPIImpl.checkAuth(userid, password, cert, MetadataAPIImpl.getPrivilegeName("insert","concept"))) {
	    MetadataAPIImpl.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.INSERTOBJECT,conceptJson,AuditConstants.FAIL,"",nameVal)
	    requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:UPDATE not allowed for this user").toString )
	  } else {
	    val apiResult = MetadataAPIImpl.AddConcepts(conceptJson,formatType,userid)
	    requestContext.complete(apiResult)
    }
	}
}
