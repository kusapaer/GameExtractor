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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HHA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HHA() {

    super("HHA", "HHA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Penny Arcade: On The Rain-Slick Precipice of Darkness: Episode 1",
        "Penny Arcade: On The Rain-Slick Precipice of Darkness: Episode 2",
        "The Maw");
    setExtensions("hha"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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

      // Header
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());
      int byte3 = ByteConverter.unsign(fm.readByte());
      int byte4 = ByteConverter.unsign(fm.readByte());

      if (byte1 == 79 && byte2 == 243 && byte3 == 47 && byte4 == 172) {
        rating += 50;
      }

      // Unknown (65536)
      if (fm.readInt() == 65536) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
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

      ExporterPlugin exporterDeflate = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (79,243,47,172)
      // 4 - Unknown (65536)
      fm.skip(8);

      // 4 - Filename Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // skip the filename directory
      fm.skip(dirLength);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      int[] directoryNameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Directory Name Offset (relative to the start of the filename directory)
        int directoryNameOffset = fm.readInt() + 16;
        FieldValidator.checkOffset(directoryNameOffset, arcSize);
        directoryNameOffsets[i] = directoryNameOffset;

        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + 16;
        try {
          FieldValidator.checkOffset(filenameOffset, dirLength + 16);
        }
        catch (Throwable t) {
          filenameOffset = directoryNameOffset;
          directoryNameOffsets[i] = 0;
        }
        filenameOffsets[i] = filenameOffset;

        // 4 - Compression Flag (0=Not Compressed, 1=Deflate, 2=Unknown Compression)
        int compressed = fm.readInt();

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompressedLength = fm.readInt();
        FieldValidator.checkLength(decompressedLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (compressed == 2) {
          offset += 8;
        }

        if (compressed == 1) {
          // Deflate Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, "", offset, length, decompressedLength, exporterDeflate);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, "", offset, length, decompressedLength);
        }

        TaskProgressManager.setValue(i);
      }

      fm.seek(16);

      // get the directory names
      String[] dirNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        int dirNameOffset = directoryNameOffsets[i];
        if (dirNameOffset == 0) {
          dirNames[i] = "";
          continue;
        }
        fm.seek(dirNameOffset);

        // X - Filename (null)
        String dirName = fm.readNullString();
        FieldValidator.checkFilename(dirName);

        dirNames[i] = dirName + "/";
      }

      // get the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        filename = dirNames[i] + filename;

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
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
