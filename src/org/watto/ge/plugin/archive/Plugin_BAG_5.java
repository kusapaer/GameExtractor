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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BAG_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BAG_5() {

    super("BAG_5", "BAG_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("NOX");
    setExtensions("bag"); // MUST BE LOWER CASE
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
        rating += 25;
      }

      getDirectoryFile(fm.getFile(), "idx");
      rating += 25;

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

      long arcSize = path.length();

      File sourcePath = getDirectoryFile(path, "idx");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Multiple?
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      long idxSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;
      long offset = 0;
      while (realNumFiles < numFiles && fm.getOffset() < idxSize) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);

        // 4 - Number of Files in this Directory
        int numFilesInDir = fm.readInt();
        FieldValidator.checkRange(numFilesInDir, 0, numFiles);

        // Loop through directory
        for (int i = 0; i < numFilesInDir; i++) {

          // 1 - Filename Length (including this field)
          int filenameLength = ByteConverter.unsign(fm.readByte()) - 1;
          FieldValidator.checkPositive(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // 2 - Flags?
          fm.skip(2);

          // 4 - File Length?
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Length?
          fm.skip(4);

          FieldValidator.checkOffset(offset, arcSize);

          offset += length;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
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
