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


package net.sf.freecol.client.gui.option;


/**
 * Interface for classes which temporarily stores changes for an
 * <code>Option</code>. Calling {@link #updateOption} should update
 * the {@link net.sf.freecol.common.option.Option} with that new
 * information.
 */
public interface OptionUpdater {



    /**
     * Updates the value of the {@link net.sf.freecol.common.option.Option}
     * this object keeps.
     */
    public void updateOption();
    
    /**
     * Reset with the value from the option.
     */
    public void reset();

    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister();
}
