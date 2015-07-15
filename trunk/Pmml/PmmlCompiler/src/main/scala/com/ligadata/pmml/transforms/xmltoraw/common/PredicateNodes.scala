package com.ligadata.pmml.transforms.xmltoraw.common

import scala.collection.mutable.ArrayBuffer
import org.xml.sax.Attributes
import com.ligadata.pmml.compiler._
import com.ligadata.pmml.traits._
import com.ligadata.pmml.support._
import com.ligadata.pmml.syntaxtree.raw.common._
import com.ligadata.pmml.transforms.xmltoraw.common._

class CompoundPredicatePmmlNodeGenerator extends PmmlNodeGenerator {

	/**
	    With the supplied xml arguments build a PmmlNode and return it to the dispatcher that is calling.
 	    @param namespaceURI: String 
 	    @param localName: String 
 	    @param qName:String
 	    @param atts: Attributes
 	    @param lineNumber : Int
 	    @param columnNumber : Int
 	    @return a PmmlNode
	 */
	 
	def make(namespaceURI: String
			, localName: String 
			, qName:String 
			, atts: Attributes
			, lineNumber : Int
			, columnNumber : Int) : PmmlNode =  {
		val ofInterest : ArrayBuffer[String] = ArrayBuffer("booleanOperator")
		val selectedValues = PmmlNode.hlpOrganizeAttributes(atts, ofInterest).asInstanceOf[ArrayBuffer[_]]
		val booleanOperator : String = selectedValues.apply(0).asInstanceOf[String]
		new PmmlCompoundPredicate(namespaceURI, localName , qName, lineNumber, columnNumber, booleanOperator)
	}
}

class SimplePredicatePmmlNodeGenerator extends PmmlNodeGenerator {

	/**
	    With the supplied xml arguments build a PmmlNode and return it to the dispatcher that is calling.
 	    @param namespaceURI: String 
 	    @param localName: String 
 	    @param qName:String
 	    @param atts: Attributes
 	    @param lineNumber : Int
 	    @param columnNumber : Int
 	    @return a PmmlNode
	 */
	 
	def make(namespaceURI: String
			, localName: String 
			, qName:String 
			, atts: Attributes
			, lineNumber : Int
			, columnNumber : Int) : PmmlNode =  {
		val ofInterest : ArrayBuffer[String] = ArrayBuffer("field", "operator", "value")
		val selectedValues = PmmlNode.hlpOrganizeAttributes(atts, ofInterest).asInstanceOf[ArrayBuffer[_]]
		val field : String = selectedValues.apply(0).asInstanceOf[String]
		val operator : String = selectedValues.apply(1).asInstanceOf[String]
		val value : String = selectedValues.apply(2).asInstanceOf[String]
		new PmmlSimplePredicate(namespaceURI, localName , qName, lineNumber, columnNumber, field, operator, value)
	}
}

class SimpleSetPredicatePmmlNodeGenerator extends PmmlNodeGenerator {

	/**
	    With the supplied xml arguments build a PmmlNode and return it to the dispatcher that is calling.
 	    @param namespaceURI: String 
 	    @param localName: String 
 	    @param qName:String
 	    @param atts: Attributes
 	    @param lineNumber : Int
 	    @param columnNumber : Int
 	    @return a PmmlNode
	 */
	 
	def make(namespaceURI: String
			, localName: String 
			, qName:String 
			, atts: Attributes
			, lineNumber : Int
			, columnNumber : Int) : PmmlNode =  {
		val ofInterest : ArrayBuffer[String] = ArrayBuffer("field", "booleanOperator")
		val selectedValues = PmmlNode.hlpOrganizeAttributes(atts, ofInterest).asInstanceOf[ArrayBuffer[_]]
		val field : String = selectedValues.apply(0).asInstanceOf[String]
		val booleanOperator : String = selectedValues.apply(1).asInstanceOf[String]
		val value : String = selectedValues.apply(2).asInstanceOf[String]
		new PmmlSimpleSetPredicate(namespaceURI, localName, qName, lineNumber, columnNumber, field, booleanOperator)	
	}
}

