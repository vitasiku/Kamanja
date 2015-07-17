package com.ligadata.pmml.transforms.printers.scala.common

import scala.collection.mutable._
import scala.math._
import scala.collection.immutable.StringLike
import scala.util.control.Breaks._
import com.ligadata.pmml.runtime._
import org.apache.log4j.Logger
import com.ligadata.fatafat.metadata._
import com.ligadata.pmml.compiler._
import com.ligadata.pmml.support._
import com.ligadata.pmml.traits._
import com.ligadata.pmml.syntaxtree.cooked.common._


class ApplyCodePrinter(ctx : PmmlContext) extends CodePrinter with com.ligadata.pmml.compiler.LogTrait {

	/**
	 *  Answer a string (code representation) for the supplied node.
	 *  @param node the PmmlExecNode
	 *  @param the CodePrinterDispatch to use should recursion to child nodes be required.
 	    @param the kind of code fragment to generate...any 
 	    	{VARDECL, VALDECL, FUNCCALL, DERIVEDCLASS, RULECLASS, RULESETCLASS , MININGFIELD, MAPVALUE, AGGREGATE, USERFUNCTION}
	 *  @order the traversalOrder to traverse this node...any {INORDER, PREORDER, POSTORDER} 
	 *  
	 *  @return some string representation of this node
	 */
	def print(node : Option[PmmlExecNode]
			, generator : CodePrinterDispatch
			, kind : CodeFragment.Kind
			, traversalOrder : Traversal.Order) : String = {

		val xnode : xApply = node match {
			case Some(node) => {
				if (node.isInstanceOf[xApply]) node.asInstanceOf[xApply] else null
			}
			case _ => null
		}

		val printThis = if (xnode != null) {
			codeGenerator(xnode, generator, kind, traversalOrder)
		} else {
			if (node != null) {
				PmmlError.logError(ctx, s"For ${xnode.qName}, expecting an xApply... got a ${xnode.getClass.getName}... check CodePrinter dispatch map initialization")
			}
			""
		}
		printThis
	}
	

	private def codeGenerator(node : xApply
							, generator : CodePrinterDispatch
							, kind : CodeFragment.Kind
							, traversalOrder : Traversal.Order) : String = 	{

		val fcn : String = traversalOrder match {
			case Traversal.INORDER => { "" }
			case Traversal.POSTORDER => { "" }
			case Traversal.PREORDER => {

				/** for iterables ... push the function type info state on stack for use by xConstant printer when handling 'ident' types */
				if (node.typeInfo != null && node.typeInfo.fcnTypeInfoType != FcnTypeInfoType.SIMPLE_FCN) {
					ctx.fcnTypeInfoStack.push(node.typeInfo)
				}
				
				val fcnStr : String = NodePrinterHelpers.applyHelper(node, ctx, generator, kind, traversalOrder)

				if (node.typeInfo != null && node.typeInfo.fcnTypeInfoType != FcnTypeInfoType.SIMPLE_FCN && ctx.fcnTypeInfoStack.nonEmpty) {
					val fcnTypeInfo : FcnTypeInfo = ctx.fcnTypeInfoStack.top
					logger.debug(s"finished printing apply function $node.function... popping FcnTypeInfo : \n${fcnTypeInfo.toString}")
					ctx.fcnTypeInfoStack.pop
				} 
				
				fcnStr
			}
		}
		fcn
	}
}

