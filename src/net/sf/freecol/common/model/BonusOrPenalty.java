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

package net.sf.freecol.common.model;

import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * The class <code>BonusOrPenalty</code> is used as the source of an
 * <code>Ability</code> or <code>Modifier</code> that is defined by
 * code rather than the Specification. See
 * <code>SimpleCombatModel</code> for examples.
 *
 * @see SimpleCombatModel
 */
public class BonusOrPenalty extends FreeColGameObjectType {

    public BonusOrPenalty(String id) {
        setId(id);
    }

}