//
// DIAGNOSTICS PACKAGE CLASS
// =========================
// Parses and loads Diagnostic Resources Packages for function blocks. 
//
// (c) AUT University - 2020
//
// Documentation
// =============
// Designers can create diagnostic resources for each function block. These are stored in XML-format files with the same
// name as the function block definition .fbt file but with the extension .drp (short for Diagnostic Resource Package).
//
// Full documentation for this class is contained in the ... <RA_BRD
//
// For more information on the SAX XML parser, see https://examples.javacodegeeks.com/core-java/xml/java-xml-parser-tutorial/ 
//
// Revision History
// ================
// 10.07.2020 BRD Original version
//
package fde;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Diagnostics {
	ArrayList<String> dps = new ArrayList<String>();  // Holds a list of the named diagnostic points found for this function 
							                          // block definition.
	private ErrorHandler errorHandler = new ErrorHandler();
	
	//
	// loadDiagnostics()
	// =================
	// Loads a named Diagnostic Resource Package. If the package loads and parses
	// correctly, each of the resources available is exposed as properties and callable
	// methods.
	//
	// fileName  The fully-qualified file name and path of the diagnostic package to load. 
	//           If the file extension is not included, it is appended automatically.
	//
	// returns 	 True if the package could be parsed and loaded.
	//
	public int loadDiagnostics(String fileName) {
		int loadStatus = XMLErrorCodes.UNDEFINED;
		errorHandler.clear();
		
		if (dps.size() > 0) {
			dps.clear();
		}	
		
		if (fileName == "") {
			errorHandler.addDescription("Invalid file name or missing path");
		} else {
			try {
				// The directory path object used to access the XML file that contains the diagnostic package.
				File fbdiag = new File(fileName);  
				
				if (fbdiag.exists()) {				
					// Create and configure a SAX XML parser 
					SAXParserFactory parserFactory = SAXParserFactory.newInstance();
					// The dtd for this file is not available so ensure that the parser does
					// not return an error.
					parserFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			        javax.xml.parsers.SAXParser parser = parserFactory.newSAXParser();
			        SAXdpgParser handler = new SAXdpgParser(dps, errorHandler);
			                
			        // Pass the diagnostic package file handle to the SAX parser and
			        // start it parsing. It will call its handler functions to give us access 
			        // to the data each time it has unpacked an element, returning back here
			        // when it finishes.
			        parser.parse(fbdiag, handler);
			        
			        if (errorHandler.Description() != "") {
			        	//System.out.println(errorHandler.Description());
			        	loadStatus = XMLErrorCodes.UNEXPECTED_ERROR;
			        } else {
			        	loadStatus = XMLErrorCodes.LOADED;
			        }
				} else {
					loadStatus = XMLErrorCodes.NOT_FOUND;
				}
		      
			} catch (Exception e) {
				loadStatus = XMLErrorCodes.UNEXPECTED_ERROR;
				errorHandler.addDescription(e.getMessage());
			}
		}	
		return loadStatus;
	}
	
	//
	// countDP()
	// =========
	public int countDP() {
		return dps.size();
	}
	
	//
	// get Name()
	// ==========
	public String Name(int ptrDP) {
		if (ptrDP < dps.size()) {			
			return (String) dps.get(ptrDP);
		} else {
			return "";
		}	
	}
	
	//
	// lastErrorDescription()
	// ======================
	public String lastErrorDescription() {
		return errorHandler.lastErrorDescription;
	}
}

//
// SAXdpgParser
// ============
// SAX is an event-based sequential access parser API that provides a mechanism for reading data from an XML document. It
// is an alternative to that provided by a DOM parser. A SAX parser only needs to report each parsing event as it happens
// and the minimum memory required for a SAX parser is proportional to the maximum depth of the XML file. One major 
// advantage of the SAX parser is that when retrieving various data elements from a node, the endElement() function is 
// called when the end of a node is reached. This is a simple way of determining that there are no more elements in the
// current node that is a lot more difficult to do with the DOM parser.
//
// The SAX parser handler extends the DefaultHandler class, providing the following callback functions:
//
// startElement()  This event is triggered when a start tag is encountered.
//
// endElement()    This event is triggered when an end tag is encountered.
//
// characters()    This event is triggered when text data is encountered in elements such as 
//                 <tag> data </tag>
//
class SAXdpgParser extends DefaultHandler {
	ErrorHandler errorHandler = new ErrorHandler();
	String nodeName = "";
	String parentNode = "";	
	String currentNode = "";
	public final int NOT_FOUND = -1;
	
	List<String> dps;
	
	// 
	// Constructor
	// ===========
	// Note how the List to be filled with diagnostic points is passed in via the constructor when the 
	// handler is created:
	//
	//     SAXdpgParser handler = new SAXdpgParser(dps);
	//
	SAXdpgParser(List<String> dps, ErrorHandler errorHandler) {
		this.dps = dps;
		this.errorHandler = errorHandler;
	} 
	
	//
	// startElement()
	// ==============
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		int ptr = 0;
		String value = "";

		String packageName = "";
		String comment = "";
		String name = "";
	
		switch (qName) {
		case "FBDiag":
			currentNode = qName;
			// Primary system characteristics.
			packageName = attributes.getValue("Name");
			comment = attributes.getValue("Comment");
			break;
			
		case "DPS":
			// List of diagnostic points
			currentNode = qName;
			break;
			
		case "DP":			
			if (currentNode == "DPS") {
				// An individual diagnostic point.
				name = attributes.getValue("Name");
				if (name != "") {
					dps.add(name);
				}
			}
			break;
		}
		
		//System.out.println("startElement: " + localName + " " + qName + " currentNode = " + currentNode);
	}
		
	//
	// endElement()
	// ============
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//System.out.println("endElement: " + qName);
		
		switch (qName) {
		case "FBDiag":
		case "DPS":
			// End of the current node.
			currentNode = "";
			break;
		}
	}	
	
	//
	// characters()
	// ============
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
      // System.out.println("characters: " + String.copyValueOf(ch, start, length).trim());
	}	
}


