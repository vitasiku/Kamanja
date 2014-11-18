#!/bin/bash
exec scala "$0" "$@"
!#

/***************************************************************************************
 * extractUdfLibMetadata.scala processes a Pmml UDF project (sbt) extracting metadata
 * for each method found in the supplied  full pkg qualified object name producing
 * Json output suitable for ingestion by the MetadataAPI.  Both the types that are 
 * required as well as the method json is generated.
 *
 **************************************************************************************/

import scala.collection.mutable._
import scala.collection.immutable.Seq
import scala.io.Source
import sys.process._
import java.util.regex.Pattern
import java.util.regex.Matcher


object extractUdfLibMetadata extends App {
  
    def usage : String = {
"""
extractUdfLibMetadata.scala --sbtProject <projectName> 
                       		--fullObjectPath <full pkg qualifed object name> 
                       		--namespace <namespace to use when generating JSON fcn objects> 
                       		--versionNumber <numeric version to use>
        where sbtProject is the sbt project name (presumably with udfs in one of its object definitions)
              fullObjectPath is the object that contains the methods for which to generate the json metadata
              namespace is the namespace to use for the udfs in the object's generated json
              versionNumber is the version number to use for the udfs in the object's generated json
      
        Notes: This script must run from the top level sbt project directory (e.g., ~/github/RTD/trunk)
        The object argument supplied must inherit from com.ligadata.pmml.udfs.UdfBase for extraction to be successful.
        Obviously it is helpful if the project actually builds.  This script executes the fat jar version of the 
        MethodExtractor (<sbtRoot>/trunk/Pmml/MethodExtractor/target/scala-2.10/MethodExtractor-1.0)

      
"""
    }


    override def main (args : Array[String]) {

        if (args.length == 0) {
        	println("No arguments supplied...Usage:")
        	println(usage)
        	sys.exit(1)
        }

        val arglist = args.toList
        type OptionMap = Map[Symbol, String]
        //println(arglist)
        def nextOption(map: OptionMap, list: List[String]): OptionMap = {
          list match {
            case Nil => map
            case "--sbtProject" :: value :: tail =>
              nextOption(map ++ Map('sbtProject -> value), tail)
            case "--fullObjectPath" :: value :: tail =>
              nextOption(map ++ Map('fullObjectPath -> value), tail)
            case "--namespace" :: value :: tail =>
              nextOption(map ++ Map('namespace -> value), tail)
            case "--versionNumber" :: value :: tail =>
              nextOption(map ++ Map('versionNumber -> value), tail)
            case option :: tail =>
              println("Unknown option " + option)
              println(usage)
              sys.exit(1)
          }
        }
    
        val options = nextOption(Map(), arglist)
        
        val sbtProject = if (options.contains('sbtProject)) options.apply('sbtProject) else null
        val fullObjectPath = if (options.contains('fullObjectPath)) options.apply('fullObjectPath) else null
        val namespace = if (options.contains('namespace)) options.apply('namespace) else null
        val versionNumber = if (options.contains('versionNumber)) options.apply('versionNumber) else null
        
        val reasonableArguments : Boolean = (sbtProject != null && fullObjectPath != null && namespace != null && versionNumber != null)
        if (! reasonableArguments) {
            println("Invalid arguments...Usage:")
            println(usage)
            sys.exit(1)
        }
        var versionNo : Int = 1
        try {
        	versionNo = versionNumber.toInt
        } catch {
        	case _ : Throwable => versionNo = 100
        }
        val versionNoArg : String = versionNo.toString

        val pwd : String = Process(s"pwd").!!.trim
		if (pwd == null) {
			println(s"Error: Unable to obtain the value of the present working directory")
			return
		}
   
		//val depJarsCmd = s"/home/rich/bin/sbtProjDependencies.scala --sbtDeps ${'"'}`sbt 'show PmmlUdfs/fullClasspath' | grep 'List(Attributed'`${'"'} --emitJarNamesOnlyList 1"
		val depJarsCmd = s"sbtProjDependencies.scala --sbtDeps ${'"'}`sbt 'show PmmlUdfs/fullClasspath' | grep 'List(Attributed'`${'"'} --emitJarNamesOnlyList 1"
		val depJarsCmdSeq : Seq[String] = Seq("bash", "-c", depJarsCmd)
		// val classPathCmd = s"/home/rich/bin/sbtProjDependencies.scala --sbtDeps ${'"'}`sbt 'show PmmlUdfs/fullClasspath' | grep 'List(Attributed'`${'"'} --emitCp 1"
		val classPathCmd = s"sbtProjDependencies.scala --sbtDeps ${'"'}`sbt 'show PmmlUdfs/fullClasspath' | grep 'List(Attributed'`${'"'} --emitCp 1"
		val classPathCmdSeq : Seq[String] = Seq("bash", "-c", classPathCmd)

		//println(s"depJarsCmd = $depJarsCmd")
		val depJarsStr : String = Process(depJarsCmdSeq).!!.trim
		val quotedDepJarsStr : String = s"${'"'}$depJarsStr${'"'}"
		//println(s"depJarsStr = $depJarsStr")
		
		//println(s"classPathCmd = $classPathCmd")
		val classPathStr : String = Process(classPathCmdSeq).!!.trim
		//println(s"classPathStr = $classPathStr")

		val extractCmd = s"$pwd/Pmml/MethodExtractor/target/scala-2.10/MethodExtractor-1.0"
		val extractCmdSeq : Seq[String] = Seq("java", "-jar", extractCmd, 
			"--object", fullObjectPath,"--namespace", namespace,"--versionNumber", versionNumber,"--deps" , quotedDepJarsStr)
		val jsonMetadataStr : String = Process(extractCmdSeq).!!.trim
		println(jsonMetadataStr)


		//println
		//println("complete!")
	}
}


extractUdfLibMetadata.main(args)
