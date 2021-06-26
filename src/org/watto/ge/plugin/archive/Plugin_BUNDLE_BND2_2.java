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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BUNDLE_BND2_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BUNDLE_BND2_2() {

    super("BUNDLE_BND2_2", "BUNDLE_BND2_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Burnout Paradise: The Ultimate Box");
    setExtensions("bundle", "bndl"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("dxt", "DXT Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("bnd2")) {
        rating += 50;
      }

      // version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Header length
      if (fm.readInt() == 48) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() == arcSize) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (bnd2)
      // 4 - Version (2)
      // 4 - Unknown (1)
      // 4 - Header Length (48)
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Header Length (48)
      fm.skip(4);

      // 4 - Properties Directory Offset
      int propertiesDirOffset = fm.readInt();
      FieldValidator.checkOffset(propertiesDirOffset, arcSize + 1);

      // 4 - First File Data Offset
      int relativeOffset = fm.readInt();
      FieldValidator.checkOffset(relativeOffset, arcSize + 1);

      // 4 - Archive Length
      // 4 - Unknown (7)
      // 8 - null
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        // 12 - null
        fm.skip(16);

        // 4 - Decompressed Properties Length (XOR last byte by 32)
        byte[] decompPropertyLengthBytes = fm.readBytes(4);
        decompPropertyLengthBytes[3] = 0;
        int decompPropertyLength = IntConverter.convertLittle(decompPropertyLengthBytes);

        // 4 - Decompressed File Length (XOR last byte by 64)
        byte[] decompLengthBytes = fm.readBytes(4);
        decompLengthBytes[3] = 0;
        int decompLength = IntConverter.convertLittle(decompLengthBytes);

        // 4 - null
        fm.skip(4);

        // 4 - Compressed Properties Length
        int propertyLength = fm.readInt();
        FieldValidator.checkLength(propertyLength, arcSize);

        // 4 - Compressed Data Length (not including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - Properties Offset (relative to the start of the Properties Directory)
        int propertyOffset = fm.readInt() + propertiesDirOffset;
        FieldValidator.checkOffset(propertyOffset, arcSize + 1);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize + 1);

        // 16 - null
        fm.skip(16);

        String filename = Resource.generateFilename(i);

        if (propertyLength != 0) {
          // has properties
          if (length != 0) {
            // has both length and properties

            long[] offsets = new long[] { propertyOffset, offset };
            long[] lengths = new long[] { propertyLength, length };
            long[] decompLengths = new long[] { decompPropertyLength, decompLength };

            BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, offsets, lengths, decompLengths);

            int totalLength = length + propertyLength;
            int totalDecompLength = decompLength + decompPropertyLength;

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, totalLength, totalDecompLength, blockExporter);
          }
          else {
            // only has properties

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, propertyOffset, propertyLength, decompPropertyLength, exporter);
          }
        }
        else {
          if (length != 0) {
            // only has length

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            // has nothing

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, 0);
          }
        }

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

    if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0) {
      return "dxt";
    }

    return null;
  }

}
