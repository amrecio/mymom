/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

/**
* Interface for classes that listen for loading actions
*/
public interface LoadingListener {


    public void loadedUnit(Unit unit);

    //public void unloadedUnit(Unit unit);

    public void loadedGoods(Goods goods);

    //public void unloadedGoods(Goods goods);

}
