/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PBO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PBO() {

    super("PBO", "PBO");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pbo");
    setGames("ArmA: Cold War Assault",
        "Operation Flashpoint");
    setPlatforms("PC");

    setTextPreviewExtensions("inc", "sqf", "sqs", "fsm", "prj", "hpp", "bikb", "bisurf", "lip", "i", "ext"); // LOWER CASE

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long offsetPos = 0;
      int realNumFiles = 0;
      int readLength = 0;
      while (offsetPos == 0) {
        // X - Filename (null)
        String filename = fm.readNullString();

        if (filename.equals("")) {
          // end of directory
          //offsetPos = (int) fm.getOffset() + 19;
          offsetPos = (int) fm.getOffset() + 20;
        }
        else {

          // 16 - Unknown
          fm.skip(16);

          // 4 - Raw File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, -1, length);

          TaskProgressManager.setValue(readLength);
          readLength += length;
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // Calculate File Offsets
      long currentPos = offsetPos;
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(currentPos);
        currentPos += resource.getLength();
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}