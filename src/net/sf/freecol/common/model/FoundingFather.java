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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Represents one FoundingFather to be contained in a Player object.
 * The FoundingFather is able to grant new abilities or bonuses to the
 * player, or to cause certain events.
 */
public class FoundingFather extends FreeColGameObjectType {

    private static int nextIndex = 0;
    
    /**
     * The probability of this FoundingFather being offered for selection.
     */
    private int[] weight = new int[4];

    /**
     * The type of this FoundingFather. One of the following constants.
     */
    private FoundingFatherType type;

    /**
     * Players that want to elect this founding father must match all
     * scopes.
     */
    private List<Scope> scopes = new ArrayList<Scope>();

    /**
     * Describe events here.
     */
    private List<Event> events = new ArrayList<Event>();

    /**
     * Holds the upgrades of Units caused by this FoundingFather.
     */
    private Map<UnitType, UnitType> upgrades;

    public static enum FoundingFatherType { TRADE, EXPLORATION, MILITARY,
            POLITICAL, RELIGIOUS }

    /**
     * A list of AbstractUnits generated by this FoundingFather.
     */
    private List<AbstractUnit> units;

    /**
     * Creates a new <code>FoundingFather</code> instance.
     *
     */
    public FoundingFather() {
        setIndex(nextIndex++);
        setModifierIndex(Modifier.FATHER_PRODUCTION_INDEX);
    }

    /**
     * Return the type of this FoundingFather.
     *
     * @return an <code>int</code> value
     */
    public FoundingFatherType getType() {
        return type;
    }
    
    /**
     * Return the localized type of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getTypeKey() {
        return getTypeKey(type);
    }

    /**
     * Return the localized type of the given FoundingFather.
     *
     * @param type an <code>int</code> value
     * @return a <code>String</code> value
     */
    public static String getTypeKey(FoundingFatherType type) {
        return "model.foundingFather." + type.toString().toLowerCase(Locale.US);
    }

    /**
     * Get the weight of this FoundingFather. This is used to select a
     * random FoundingFather.
     *
     * @param age an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getWeight(int age) {
        switch(age) {
        case 1:
            return weight[1];
        case 2:
            return weight[2];
        case 3:
        default:
            return weight[3];
        }
    }

    /**
     * Get the <code>Units</code> value.
     *
     * @return a <code>List<AbstractUnit></code> value
     */
    public final List<AbstractUnit> getUnits() {
        return units;
    }

    /**
     * Set the <code>Units</code> value.
     *
     * @param newUnits The new Units value.
     */
    public final void setUnits(final List<AbstractUnit> newUnits) {
        this.units = newUnits;
    }

    /**
     * Get the <code>Events</code> value.
     *
     * @return a <code>List<Event></code> value
     */
    public final List<Event> getEvents() {
        return events;
    }

    /**
     * Set the <code>Events</code> value.
     *
     * @param newEvents The new Events value.
     */
    public final void setEvents(final List<Event> newEvents) {
        this.events = newEvents;
    }

    /**
     * Get the <code>Scopes</code> value.
     *
     * @return a <code>List<Scope></code> value
     */
    public final List<Scope> getScopes() {
        return scopes;
    }

    /**
     * Returns true if this <code>FoundingFather</code> is available
     * to the Player given.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAvailableTo(Player player) {
        if (player.isEuropean()) {
            if (scopes == null || scopes.isEmpty()) {
                return true;
            } else {
                for (Scope scope : scopes) {
                    if (scope.appliesTo(player)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get the <code>Upgrades</code> value.
     *
     * @return a <code>Map<UnitType, UnitType></code> value
     */
    public final Map<UnitType, UnitType> getUpgrades() {
        return upgrades;
    }

    /**
     * Set the <code>Upgrades</code> value.
     *
     * @param newUpgrades The new Upgrades value.
     */
    public final void setUpgrades(final Map<UnitType, UnitType> newUpgrades) {
        this.upgrades = newUpgrades;
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        String typeString = in.getAttributeValue(null, "type").toUpperCase(Locale.US);
        type = Enum.valueOf(FoundingFatherType.class, typeString);

        weight[1] = Integer.parseInt(in.getAttributeValue(null, "weight1"));
        weight[2] = Integer.parseInt(in.getAttributeValue(null, "weight2"));
        weight[3] = Integer.parseInt(in.getAttributeValue(null, "weight3"));

    }

    public void readChildren(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Event.getXMLElementTagName().equals(childName)) {
                Event event = new Event();
                event.readFromXML(in, specification);
                events.add(event);
            } else if ("scope".equals(childName)) {
                scopes.add(new Scope(in));
            } else if ("unit".equals(childName)) {
                AbstractUnit unit = new AbstractUnit(in); // AbstractUnit closes element
                if (units == null) {
                    units = new ArrayList<AbstractUnit>();
                }
                units.add(unit);
            } else if ("upgrade".equals(childName)) {
                UnitType fromType = specification.getUnitType(in.getAttributeValue(null, "from-id"));
                UnitType toType = specification.getUnitType(in.getAttributeValue(null, "to-id"));
                if (fromType != null && toType != null) {
                    if (upgrades == null) {
                        upgrades = new HashMap<UnitType, UnitType>();
                    }
                    upgrades.put(fromType, toType);
                }
                in.nextTag();
            } else {
                super.readChild(in, specification);
            }
        }

    }


    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXMLImpl(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("type", type.toString().toLowerCase(Locale.US));
        for (int index = 1; index <= 3; index++) {
            out.writeAttribute("weight" + index, Integer.toString(weight[index]));
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        if (events != null) {
            for (Event event : events) {
                event.toXMLImpl(out);
            }
        }
        if (scopes != null) {
            for (Scope scope : scopes) {
                scope.toXMLImpl(out);
            }
        }
        if (units != null) {
            for (AbstractUnit unit : units) {
                 out.writeStartElement("unit");
                 out.writeAttribute(ID_ATTRIBUTE_TAG, unit.getId());
                 //out.writeAttribute("role", unit.getRole().toString().toLowerCase(Locale.US));
                 //out.writeAttribute("number", String.valueOf(unit.getNumber()));
                 out.writeEndElement();
            }
        }
        if (upgrades != null) {
            for (Map.Entry<UnitType, UnitType> entry : upgrades.entrySet()) {
                out.writeStartElement("upgrade");
                out.writeAttribute("from-id", entry.getKey().getId());
                out.writeAttribute("to-id", entry.getValue().getId());
                out.writeEndElement();
            }
        }
    }

    public static String getXMLElementTagName() {
        return "founding-father";
    }


}
