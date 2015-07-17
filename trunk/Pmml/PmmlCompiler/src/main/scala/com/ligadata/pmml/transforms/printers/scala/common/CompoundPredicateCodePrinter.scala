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

class CompoundPredicateCodePrinter(ctx : PmmlContext) extends CodePrinter with com.ligadata.pmml.compiler.LogTrait {

	/**
	 *  Answer a string (code representation) for the supplied node.
	 *  @param node the PmmlExecNode
	 *  @param the CodePrinterDispatch to use should recursion to child nodes be required.
 	 *  @param the kind of code fragment to generate...any 
 	 *   	{VARDECL, VALDECL, FUNCCALL, DERIVEDCLASS, RULECLASS, RULESETCLASS , MININGFIELD, MAPVALUE, AGGREGATE, USERFUNCTION}
	 *  @order the traversalOrder to traverse this node...any {INORDER, PREORDER, POSTORDER} 
	 *  
	 *  @return some string representation of this node
	 */
	def print(node : Option[PmmlExecNode]
			, generator : CodePrinterDispatch
			, kind : CodeFragment.Kind
			, traversalOrder : Traversal.Order) : String = {

		val xnode : xCompoundPredicate = node match {
			case Some(node) => {
				if (node.isInstanceOf[xCompoundPredicate]) node.asInstanceOf[xCompoundPredicate] else null
			}
			case _ => null
		}

		val printThis = if (xnode != null) {
			codeGenerator(xnode, generator, kind, traversalOrder)
		} else {
			if (node != null) {
				PmmlError.logError(ctx, s"For ${xnode.qName}, expecting an xCompoundPredicate... got a ${xnode.getClass.getName}... check CodePrinter dispatch map initialization")
			}
			""
		}
		printThis
	}
	

	private def codeGenerator(node : xCompoundPredicate
							, generator : CodePrinterDispatch
							, kind : CodeFragment.Kind
							, traversalOrder : Traversal.Order) : String = 	{

	  	val fcnBuffer : StringBuilder = new StringBuilder()
		val compoundPredStr : String = traversalOrder match {
			case Traversal.INORDER => { "" }
			case Traversal.POSTORDER => { "" }
			case Traversal.PREORDER => {
				val boolOpFcn : String = PmmlTypes.scalaBuiltinNameFcnSelector(node.booleanOperator)
				val isShortCircuitOp = (boolOpFcn == "or" || boolOpFcn == "and")
				if (isShortCircuitOp) {
					val op : String = if (boolOpFcn == "or") "||" else "&&"
			  		fcnBuffer.append("(")
					NodePrinterHelpers.andOrFcnPrint(op
													, node
												    , ctx
												    , generator
												    , kind 
												    , Traversal.INORDER
												    , fcnBuffer
												    , null)		    
			  		fcnBuffer.append(")")
			  		fcnBuffer.toString
				} else {
					val cPred = s"$boolOpFcn("
					fcnBuffer.append(cPred)
					var i : Int = 0
	
			  		node.Children.foreach((child) => {
			  			i += 1
				  		generator.generate(Some(child), fcnBuffer, CodeFragment.FUNCCALL)
				  		if (i < node.Children.length) fcnBuffer.append(", ")
			  		})
	
			  		val closingParen : String = s")"
			  		fcnBuffer.append(closingParen)
			  		fcnBuffer.toString
				}
			}
		}
		compoundPredStr
	}
}
