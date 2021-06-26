/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.task.TaskProgressManager;

public class Exporter_QuickBMSWrapper extends Exporter_Default {

  /** the script file used to read the archive **/
  String scriptPath = "";

  public String getScriptPath() {
    return scriptPath;
  }

  public void setScriptPath(String scriptPath) {
    this.scriptPath = scriptPath;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_QuickBMSWrapper(String scriptPath) {
    setName("QuickBMS Decompression");
    this.scriptPath = scriptPath;
  }

  /**
  **********************************************************************************************
  Uses QuickBMS to decompress a file
  **********************************************************************************************
  **/
  public File extractFile(Resource resource, File tempDirectory) {
    try {

      // get the QuickBMS Executable
      String quickbmsPath = QuickBMSHelper.checkAndShowPopup();
      if (quickbmsPath == null) {
        return null;
      }

      // Build a script for doing the decompression
      if (scriptPath == null || !(new File(scriptPath).exists())) {
        // problem reading the script file
        return null;
      }

      File sourceFile = resource.getSource();

      ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-o", "-f", resource.getName(), scriptPath, sourceFile.getAbsolutePath(), tempDirectory.getAbsolutePath());

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      TaskProgressManager.startTask();

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish

      // Stop the task
      TaskProgressManager.stopTask();

      if (returnCode == 0) {
        // successful decompression
        //if (destFile.exists()) {
        return tempDirectory;
        //}
      }

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  From ExporterPlugin, to do an actual extraction to a file. Special case for this plugin only!
  We normally don't overwrite this method, but just overwrite open() instead.
  **********************************************************************************************
  **/
  @Override
  public void extract(Resource source, FileManipulator destination) {
    try {
      this.exportDestination = destination;

      open(source);
      if (this.exportDestination == null) {
        // This occurs if QuickBMS has already written straight out to this file.
        // In this case, just close it all off and return, rather than duplicating the file
      }
      else {
        // normal case, want to copy the file from the QuickBMS temp location to the destination
        while (available()) {
          destination.writeByte(read());
        }
      }
      close();

      this.exportDestination = null;
    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      /*
      // Create a temporary file for exporting the raw data
      String tempFilePath = new File(Settings.get("TempDirectory")).getAbsolutePath();
      tempFilePath += File.separator + "tempforquickbms_" + source.getFilenameWithExtension();
      File tempFile = new File(tempFilePath);
      
      //
      // Export the file from the source to a temporary filename
      //
      readLength = source.getLength();
      if (readLength < 0) {
        readLength = 0; // just in case, so we don't have a never-ending "while" loop down below
      }
      
      int bufferSize = (int) readLength; // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }
      
      readSource = new FileManipulator(source.getSource(), false, bufferSize);
      readSource.seek(source.getOffset());
      
      FileManipulator tempFM = new FileManipulator(tempFile, true);
      tempFile = tempFM.getFile(); // in case the destination filename had funny characters, this will get the correct filename
      while (readLength != 0) {
        tempFM.writeByte(readSource.readByte());
        readLength--;
      }
      tempFM.close();
      
      readSource.close();
      readSource = null;
      
      //
      // Now that the file is exported, run the decompression using QuickBMS. Outputs to the correct filename
      //
      if (!tempFile.exists()) {
        readLength = 0; // force the file to "not exist"
        return;
      }
      */

      File tempPath = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
      //realFilePath += File.separator + source.getFilenameWithExtension();
      //File realFile = new File(realFilePath);
      tempPath = FilenameChecker.correctFilename(tempPath); // removes funny characters etc.

      File outputFile = new File(tempPath.getAbsolutePath() + File.separator + source.getName());
      if (outputFile.exists() && outputFile.length() <= 0) {
        // It can get here if the QuickBMS extract file and the temporary file are the same.
        if (this.exportDestination != null && outputFile.equals(this.exportDestination.getFile())) {
          // If this happens, close the (empty) temporary file, and tell QuickBMS to extract over it.
          this.exportDestination.close();
          this.exportDestination = null;
          tempPath = extractFile(source, tempPath); // tempPath will return null if there was an error in QuickBMS
        }
        // already extracted - don't extract again
      }
      else {
        // run the extraction
        tempPath = extractFile(source, tempPath); // tempPath will return null if there was an error in QuickBMS
      }

      //
      // Now open this extracted file for reading normally.
      //
      if (tempPath == null || outputFile == null || !outputFile.exists()) {
        // some kind of problem running the extraction - a QuickBMS issue
        readLength = 0; // force the file to "not exist"
        return;
      }

      readLength = outputFile.length();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(outputFile, false, bufferSize);
      readSource.seek(0); // just in case, restart at the beginning of the decompressed file

    }
    catch (Throwable t) {
    }
  }

}