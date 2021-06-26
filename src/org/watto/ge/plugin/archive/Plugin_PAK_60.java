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
import org.watto.ErrorLogger;
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate_XOR;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_60 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_60() {

    super("PAK_60", "PAK_60");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Praetorians");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ptx", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("dat", "gui", "h", "mis", "trg", "ogi"); // LOWER CASE

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

      // Header (PK header with XOR)
      if (fm.readShort() == -7941) {
        rating += 50;
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

      ExporterPlugin exporterDeflate = new Exporter_Deflate_XOR(171);
      ExporterPlugin exporterDefault = new Exporter_XOR(171);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 171));

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Header (PK)
        fm.skip(2);

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 1311747) {
          // File Entry

          // 2 - Unknown (2)
          fm.skip(2);

          // 2 - Compression Method
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Filename Length
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 2 - Extra Data Length
          int extraLength = fm.readShort();
          FieldValidator.checkLength(extraLength, arcSize);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - Extra Data
          fm.skip(extraLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          if (compType == 0) {
            // uncompressed

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporterDefault);
          }
          else {
            // compressed - probably Deflate

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterDeflate);
          }
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          fm.skip(2);
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 656387) {
          // Directory Entry (Short) (or sometimes a file)

          // 2 - Unknown (20)
          fm.skip(2);

          // 2 - Unknown (2)
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          fm.skip(2);
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - File Data
          if (length != 0) {
            long offset = fm.getOffset();
            fm.skip(length);

            if (compType == 0) {
              // uncompressed

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporterDefault);
            }
            else {
              // compressed - probably Deflate

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterDeflate);
            }
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[PAK_60]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
        }

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
