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

package org.watto.ge.plugin;

import org.watto.Language;
import org.watto.component.WSObjectPlugin;
import org.watto.datatype.Resource;

public abstract class RenamerPlugin extends WSObjectPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public RenamerPlugin(String code, String name) {
    setCode(code);
    setName(name);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_RenamerPlugin");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void rename(Resource resource, String searchValue, String replaceValue);

  /**
  **********************************************************************************************
  Whether this renamer needs an additional search field on the interface
  **********************************************************************************************
  **/
  public boolean showSearchField() {
    return false;
  }

}