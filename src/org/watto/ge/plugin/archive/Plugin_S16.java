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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_S16 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_S16() {

    super("S16", "S16");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Warhammer 40K: Rites of War");
    setExtensions("s16"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("s16image", "S16 Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      //ExporterPlugin exporter = Exporter_Deflate_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int relativeOffset = 4 + (numFiles * 32);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 8 - null
        fm.skip(8);

        // 4 - Image Data Offset (relative to the start of the File Data)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Image Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed Flag (0=Uncompressed/1=Compressed)
        int compressed = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        String filename = Resource.generateFilename(i) + ".s16image";

        Resource resource = null;
        if (compressed == 1) {
          // compressed

          int decompLength = width * height * 2;

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length);
        }

        resource.addProperty("Width", width);
        resource.addProperty("Height", height);

        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
