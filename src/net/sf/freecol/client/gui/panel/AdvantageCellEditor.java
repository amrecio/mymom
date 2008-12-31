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

import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.NationType;

/**
* A table cell editor that can be used to select a nation.
*/
public final class AdvantageCellEditor extends DefaultCellEditor {


    
    /**
    * A standard constructor.
    */
    public AdvantageCellEditor() {
        super(new JComboBox(new Vector<EuropeanNationType>(FreeCol.getSpecification().getEuropeanNationTypes())));
    }
    
    public Object getCellEditorValue() {
        return ((JComboBox) getComponent()).getSelectedItem();
    }
}
