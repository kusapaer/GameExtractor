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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
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
public class Plugin_PAK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_2() {

    super("PAK_2", "PAK_2");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("pak");
    setGames("Dune 2",
        "Eye of the Beholder");
    setPlatforms("PC");

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

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int firstFileOffset = fm.readInt() - 4;
      FieldValidator.checkOffset(firstFileOffset, arcSize);
      fm.seek(0);

      int i = 0;
      while (fm.getOffset() < firstFileOffset) {
        // 4 - fileOffset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename (null term)
        //String filename = fm.readNullString();
        long namePos = fm.getOffset();
        String filename = fm.readNullString(512);
        int nameLength = filename.length();
        if (nameLength == 512) {
          return null; // not the right kind of archive
        }
        else {
          fm.relativeSeek(namePos + nameLength + 1);
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength);

        TaskProgressManager.setValue((int) fm.getOffset());
        i++;
      }

      // Calculate file sizes
      for (int j = 0; j < i; j++) {
        if (j == i - 1) {
          resources[j].setLength((int) (arcSize - resources[j].getOffset()));
        }
        else {
          resources[j].setLength((int) (resources[j + 1].getOffset() - resources[j].getOffset()));
          FieldValidator.checkLength(resources[j].getLength(), arcSize);
        }
      }

      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}