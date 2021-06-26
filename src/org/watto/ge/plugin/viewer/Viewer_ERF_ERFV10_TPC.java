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

package org.watto.ge.plugin.viewer;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ERF_ERFV10;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ERF_ERFV10_TPC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ERF_ERFV10_TPC() {
    super("ERF_ERFV10_TPC", "ERF_ERFV10 TPC Image");
    setExtensions("tpc");

    setGames("Star Wars: Knights Of The Old Republic");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_ERF_ERFV10) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - File Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
        rating += 5;
      }

      fm.skip(4);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 4 - Image Data Length
      int imageLength = fm.readInt();

      // 2 - Unknown
      // 2 - Unknown
      short formatCode1 = fm.readShort();
      short formatCode2 = fm.readShort();

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (260)
      // 112 - null
      int formatCode3 = ByteConverter.unsign(fm.readByte());
      int formatCode4 = ByteConverter.unsign(fm.readByte());
      fm.skip(114);

      // X - Pixel Data (DXT5 or RGBA)
      ImageResource imageResource = null;
      if (imageLength == 0) {
        // RGBA or RGB
        //if (formatCode1 == 0 && formatCode2 == 16256) {
        if (formatCode3 == 2) {
          imageResource = ImageFormatReader.readRGB(fm, width, height);
          //System.out.println(formatCode1 + "\t" + formatCode2 + "\t" + formatCode3 + "\t" + formatCode4 + "\t" + "RGB");
        }
        else {
          imageResource = ImageFormatReader.readRGBA(fm, width, height);
          //System.out.println(formatCode1 + "\t" + formatCode2 + "\t" + formatCode3 + "\t" + formatCode4 + "\t" + "RGBA");
        }
      }
      else {
        //DXT5 or DXT1
        if (imageLength == width * height || formatCode3 == 4) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
          //System.out.println(formatCode1 + "\t" + formatCode2 + "\t" + formatCode3 + "\t" + formatCode4 + "\t" + "DXT5");
        }
        else if ((width * height) / imageLength == 6) {
          // cube
          //System.out.println(formatCode1 + "\t" + formatCode2 + "\t" + formatCode3 + "\t" + formatCode4 + "\t" + "DXT_CUBE");
          return null;
        }
        else {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
          //System.out.println(formatCode1 + "\t" + formatCode2 + "\t" + formatCode3 + "\t" + formatCode4 + "\t" + "DXT1");
        }
      }

      // flip vertically
      imageResource = ImageFormatReader.flipVertically(imageResource);

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {

  }

}