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
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAC_CA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAC_CA() {

    super("PAC_CA", "PAC_CA");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("pac");
    setGames("Pacman: Adventures In Time");
    setPlatforms("PC");

    setFileTypes("twt", "Text Document",
        "tod", "3D Object Mesh",
        "jbf", "Image Thumbnail Browser",
        "map", "Maze 3D Mesh",
        "mcf", "Maze Configuration Settings",
        "pms", "Programming Script");

    setTextPreviewExtensions("mcf", "pms", "twt"); // LOWER CASE

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

      // Header
      if (fm.readString(4).equals(" CA ")) {
        rating += 50;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Directory Offset (16)
      if (fm.readInt() == 16) {
        rating += 5;
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

      // 4 - Header
      fm.skip(16);

      // 4 - 100

      // 4 - null

      // 4 - 16 (Dir Offset)

      // 4 - Number of Directories
      int numDir = fm.readInt();
      FieldValidator.checkNumFiles(numDir);

      String[] directories = new String[numDir];
      int[] numFilesInDir = new int[numDir];

      int numFiles = 0;
      for (int i = 0; i < numDir; i++) {
        // 4 - Number of Files?
        numFilesInDir[i] = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInDir[i]);

        // 32 - Directory Name
        directories[i] = fm.readNullString(32);
        FieldValidator.checkFilename(directories[i]);

        // 4 - Offset?
        fm.skip(4);

        numFiles += numFilesInDir[i];
      }

      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long firstOffset = 999;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 32 - Filename
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();

        if (fm.getOffset() >= firstOffset) {
          resources = resizeResources(resources, i);
          i = numFiles;
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);

          if (i == 0) {
            firstOffset = offset;
          }

          // 4 - File Length
          long lengthPointerLocation = fm.getOffset();
          long lengthPointerLength = 4;

          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue(i);
        }
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