//
// FILE INPUT/OUTPUT LIBRARY
// =========================
// Library of generic functions for reading and writing sequential files.
//
// AUT University - 2019-2020
//
// Revision History
// ================
// 21.10.2019 BRD Original version.
//
package fde;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

enum FileIOstatus {
	UNDEFINED,
	INVALID_FILE_NAME,
	INVALID_FILE_PATH,
	FILE_NOT_FOUND,
	FILE_CREATED,
	WRITE_FAILURE,
	CLOSE_FAILURE
}

public class FileIO {
	BufferedWriter fileIO;
	String path = "";
	String fileName = "";
	String errorDescription = "";
	
	//
	// createFile()
	// ============
	public FileIOstatus createFile(String path, String fileName) {		
		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
		errorDescription = "";
		
		if (path == "") {
			errorDescription = "File path not specified";
			IOstatus = FileIOstatus.INVALID_FILE_PATH;
		} else if (fileName == "") {
			errorDescription = "File name not specified";
			IOstatus = FileIOstatus.INVALID_FILE_NAME;
		} else {		
			try {
				this.path = path;
				this.fileName = fileName;
				FileWriter file = new FileWriter(path + fileName);
				fileIO = new BufferedWriter(file);
				IOstatus = FileIOstatus.FILE_CREATED;
						
			} catch (IOException e){
				errorDescription = "Error in createFile() while attempting to create file '" +
								    fileName + "' in folder '" + path + "'. " + e.getMessage();
			}
		}	
		return IOstatus;
	}
	
	// 
	// write()
	// =======
	public FileIOstatus write(String lineText) {
		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
		errorDescription = "";
		
		try {
			fileIO.write(lineText);
		} catch (IOException e){
			IOstatus = FileIOstatus.WRITE_FAILURE;
			errorDescription = "Error in write(). " + e.getMessage();
		}
		return IOstatus;
	}
	
	//
	// close()
	// =======
	public FileIOstatus close() {
		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
		
		try {
			fileIO.close();
		} catch (IOException e) {
			IOstatus = FileIOstatus.CLOSE_FAILURE;
			errorDescription = "Error during close(). " + e.getMessage();
		}
		return IOstatus;
	}
	
	//
	// errorDescription()
	// ==================
	public String errorDescription() {
		return errorDescription;
	}
}
