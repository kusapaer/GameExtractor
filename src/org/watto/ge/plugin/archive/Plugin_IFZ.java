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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IFZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IFZ() {

    super("IFZ", "IFZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Next Big Thing");
    setExtensions("ifz"); // ALSO *.A## *.G## *.SP# // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
        // RESOURCE*.IFZ
        if (FilenameSplitter.getFilename(fm.getFile()).indexOf("RESOURCE") == 0) {
          rating += 25;
        }
      }
      else {
        // RESOURCE*.A## RESOURCE*.G## RESOURCE*.SP#
        try {
          if (FilenameSplitter.getFilename(fm.getFile()).indexOf("RESOURCE") == 0) {

            // check for extension *.A## *.G## *.SP#
            String extension = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();
            if (extension.length() == 3) {
              if (extension.charAt(0) == 'a' || extension.charAt(0) == 'g') {
                if (Integer.parseInt(extension.substring(1)) < 99) {
                  rating += 25;
                }
              }
              else if (extension.charAt(0) == 's' && extension.charAt(1) == 'p') {
                if (Integer.parseInt(extension.substring(2)) < 9) {
                  rating += 25;
                }
              }
            }
          }
        }
        catch (Throwable t) {
        }
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number of Files [Right-Shift by 2 bits]
      int numFiles = fm.readInt() >> 2;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset - 1, arcSize);

        if (offset == arcSize) {
          offset = -1; // we exclude these files in the next loop
        }

        offsets[i] = offset;
      }

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        long offset = offsets[i];

        if (length == 0 || offset == -1) {
          // an empty file
          continue;
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
