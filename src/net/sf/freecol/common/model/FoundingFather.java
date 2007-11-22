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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one FoundingFather to be contained in a Player object.
 * The FoundingFather is able to grant new abilities or bonuses to the
 * player, or to cause certain events.
 */
public class FoundingFather extends FreeColGameObjectType implements Abilities, Modifiers {

    
    /**
     * The probability of this FoundingFather being offered for selection.
     */
    private int[] weight = new int[4];

    /**
     * The type of this FoundingFather. One of the following constants.
     */
    private int type;

    /**
     * Holds the upgrades of Units caused by this FoundingFather.
     */
    private Map<UnitType, UnitType> upgrades;

    public static final int TRADE = 0,
                            EXPLORATION = 1,
                            MILITARY = 2,
                            POLITICAL = 3,
                            RELIGIOUS = 4,
                            TYPE_COUNT = 5;

    /**
     * Stores the Features of this Type.
     */
    private HashMap<String, Feature> features = new HashMap<String, Feature>();

    /**
     * Stores the Events of this Type.
     */
    private HashMap<String, String> events = new HashMap<String, String>();

    /**
     * Stores the IDs of the Nations and NationTypes this
     * FoundingFather is available to.
     */
    private HashSet<String> availableTo = new HashSet<String>();

    /**
     * A list of AbstractUnits generated by this FoundingFather.
     */
    private List<AbstractUnit> units;

    /**
     * Creates a new <code>FoundingFather</code> instance.
     *
     * @param newIndex an <code>int</code> value
     */
    public FoundingFather(int newIndex) {
        setIndex(newIndex);
    }

    /**
     * Return the localized text of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getText() {
        return Messages.message(getId() + ".text");
    }

    /**
     * Return the localized birth and death dates of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getBirthAndDeath() {
        return Messages.message(getId() + ".birthAndDeath");
    }

    /**
     * Return the type of this FoundingFather.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return type;
    }
    
    /**
     * Return the localized type of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getTypeAsString() {
        return getTypeAsString(type);
    }

    /**
     * Return the localized type of the given FoundingFather.
     *
     * @param type an <code>int</code> value
     * @return a <code>String</code> value
     */
    public static String getTypeAsString(int type) {
        switch (type) {
            case TRADE: return Messages.message("model.foundingFather.trade");
            case EXPLORATION: return Messages.message("model.foundingFather.exploration");
            case MILITARY: return Messages.message("model.foundingFather.military");
            case POLITICAL: return Messages.message("model.foundingFather.political");
            case RELIGIOUS: return Messages.message("model.foundingFather.religious");
        }
        
        return "";
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
     * Returns true if this <code>FoundingFather</code> is available
     * to the Player given.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAvailableTo(Player player) {
        return (availableTo.isEmpty() || availableTo.contains(player.getNationID()) ||
                availableTo.contains(player.getNationType().getId()));
    }



    /**
     * Returns true if this FoundingFather has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return features.containsKey(id) && 
            (features.get(id) instanceof Ability) &&
            ((Ability) features.get(id)).getValue();
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        features.put(id, new Ability(id, newValue));
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Modifier getModifier(String id) {
        return (Modifier) features.get(id);
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        features.put(id, newModifier);
    }


    public void setFeature(Feature feature) {
        if (feature == null) {
            return;
        }
        Feature oldValue = features.get(feature.getId());
        if (oldValue instanceof Modifier && feature instanceof Modifier) {
            features.put(feature.getId(), Modifier.combine((Modifier) oldValue, (Modifier) feature));
        } else {
            features.put(feature.getId(), feature);
        }
    }

    /**
     * Returns a copy of this FoundingFather's features.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Feature> getFeatures() {
        return new HashMap<String, Feature>(features);
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

    /**
     * Returns all events.
     *
     * @return a <code>List</code> of Events.
     */
    public Map<String, String> getEvents() {
        return events;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, Map<String, UnitType> unitTypeByRef)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        String typeString = in.getAttributeValue(null, "type");
        if ("trade".equals(typeString)) {
            type = TRADE;
        } else if ("exploration".equals(typeString)) {
            type = EXPLORATION;
        } else if ("military".equals(typeString)) {
            type = MILITARY;
        } else if ("political".equals(typeString)) {
            type = POLITICAL;
        } else if ("religious".equals(typeString)) {
            type = RELIGIOUS;
        } else {
            throw new IllegalArgumentException("FoundingFather " + getId() + " has unknown type " + typeString);
        }                           

        weight[1] = Integer.parseInt(in.getAttributeValue(null, "weight1"));
        weight[2] = Integer.parseInt(in.getAttributeValue(null, "weight2"));
        weight[3] = Integer.parseInt(in.getAttributeValue(null, "weight3"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(childName)) {
                Ability ability = new Ability(in);
                if (ability.getSource() == null) {
                    ability.setSource(this.getId());
                }
                setFeature(ability);
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getId());
                }
                setModifier(modifier.getId(), modifier); // close this element
            } else if ("event".equals(childName)) {
                String eventId = in.getAttributeValue(null, "id");
                String value = in.getAttributeValue(null, "value");
                events.put(eventId, value);
                in.nextTag(); // close this element
            } else if ("nation".equals(childName) ||
                       "nation-type".equals(childName)) {
                availableTo.add(in.getAttributeValue(null, "id"));
                in.nextTag();
            } else if ("unit".equals(childName)) {
                AbstractUnit unit = new AbstractUnit(in); // AbstractUnit closes element
                if (units == null) {
                    units = new ArrayList<AbstractUnit>();
                }
                units.add(unit);
            } else if ("upgrade".equals(childName)) {
                UnitType fromType = unitTypeByRef.get(in.getAttributeValue(null, "from-id"));
                UnitType toType = unitTypeByRef.get(in.getAttributeValue(null, "to-id"));
                if (fromType != null && toType != null) {
                    if (upgrades == null) {
                        upgrades = new HashMap<UnitType, UnitType>();
                    }
                    upgrades.put(fromType, toType);
                }
                in.nextTag();
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                        !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }

    }

}
