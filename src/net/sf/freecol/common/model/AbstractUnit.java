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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Unit.Role;

/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    /**
     * The role of this AbstractUnit.
     */
    private Role role = Role.DEFAULT;

    /**
     * The number of units.
     */
    private int number = 1;

    public AbstractUnit() {
        // empty constructor
    }

    public AbstractUnit(String id, Role someRole, int someNumber) {
        setId(id);
        this.role = someRole;
        this.number = someNumber;
    }

    public AbstractUnit(UnitType unitType, Role someRole, int someNumber) {
        this(unitType.getId(), someRole, someNumber);
    }

    /**
     * Creates a new <code>AbstractUnit</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnit(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }


    /**
     * Get the <code>UnitType</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public final UnitType getUnitType() {
        return FreeCol.getSpecification().getUnitType(getId());
    }

    /**
     * Get the <code>Role</code> value.
     *
     * @return a <code>Role</code> value
     */
    public final Role getRole() {
        return role;
    }

    /**
     * Set the <code>Role</code> value.
     *
     * @param newRole The new Role value.
     */
    public final void setRole(final Role newRole) {
        this.role = newRole;
    }

    /**
     * Get the <code>Number</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getNumber() {
        return number;
    }

    /**
     * Set the <code>Number</code> value.
     *
     * @param newNumber The new Number value.
     */
    public final void setNumber(final int newNumber) {
        this.number = newNumber;
    }

    /**
     * Returns the Equipment necessary to create a Unit with the same
     * type and role as this AbstractUnit.
     *
     * @return an <code>EquipmentType[]</code> value
     */
    public EquipmentType[] getEquipment() {
        List<EquipmentType> equipment = new ArrayList<EquipmentType>();
        switch(role) {
        case PIONEER:
            EquipmentType tools = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");
            for (int count = 0; count < tools.getMaximumCount(); count++) {
                equipment.add(tools);
            }
            break;
        case MISSIONARY:
            equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.missionary"));
            break;
        case SOLDIER:
            equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.muskets"));
            break;
        case SCOUT:
            equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.horses"));
            break;
        case DRAGOON:
            equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.muskets"));
            equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.horses"));
            break;
        case DEFAULT:
        default:
        }
        return equipment.toArray(new EquipmentType[equipment.size()]);
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        role = Enum.valueOf(Role.class, getAttribute(in, "role", "default").toUpperCase());
        number = getAttribute(in, "number", 1);
        in.nextTag(); // close this element
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("id", getId());
        out.writeAttribute("role", role.toString().toLowerCase());
        out.writeAttribute("number", String.valueOf(number));
        out.writeEndElement();
    }
    
    public static String getXMLElementTagName() {
        return "abstractUnit";
    }

}

