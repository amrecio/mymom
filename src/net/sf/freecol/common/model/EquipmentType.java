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
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Unit.Role;

public class EquipmentType extends BuildableType {

    public static final EquipmentType[] NO_EQUIPMENT = new EquipmentType[0];

    /**
     * The maximum number of equipment items that can be combined.
     */
    private int maximumCount = 1;

    /**
     * Determines which type of Equipment will be lost first if the
     * Unit carrying it is defeated. Horses should be lost before
     * Muskets, for example.
     */
    private int combatLossPriority;

    /**
     * The default Role of the Unit carrying this type of Equipment.
     */
    private Role role;

    /**
     * Describe militaryEquipment here.
     */
    private boolean militaryEquipment;

    /**
     * Stores the abilities required of a unit to be equipped.
     */
    private HashMap<String, Boolean> requiredUnitAbilities = new HashMap<String, Boolean>();
    
    /**
     * Stores the abilities required of the location where the unit is
     * to be equipped.
     */
    private HashMap<String, Boolean> requiredLocationAbilities = new HashMap<String, Boolean>();
    
    /**
     * A List containing the IDs of equipment types compatible with this one.
     */
    private List<String> compatibleEquipment = new ArrayList<String>();


    public EquipmentType() {}

    public EquipmentType(int index) {
        setIndex(index);
    }

    /**
     * Get the <code>MaximumCount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximumCount() {
        return maximumCount;
    }

    /**
     * Set the <code>MaximumCount</code> value.
     *
     * @param newMaximumCount The new MaximumCount value.
     */
    public final void setMaximumCount(final int newMaximumCount) {
        this.maximumCount = newMaximumCount;
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
     * Get the <code>CombatLossPriority</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getCombatLossPriority() {
        return combatLossPriority;
    }

    /**
     * Returns true if this EquipmentType can be captured in combat.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBeCaptured() {
        return (combatLossPriority > 0);
    }

    /**
     * Set the <code>CombatLossPriority</code> value.
     *
     * @param newCombatLossPriority The new CombatLossPriority value.
     */
    public final void setCombatLossPriority(final int newCombatLossPriority) {
        this.combatLossPriority = newCombatLossPriority;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getUnitAbilitiesRequired() {
        return requiredUnitAbilities;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getLocationAbilitiesRequired() {
        return requiredLocationAbilities;
    }

    /**
     * Returns true if this type of equipment is compatible with the
     * given type of equipment.
     *
     * @param otherType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isCompatibleWith(EquipmentType otherType) {
        if (this.getId().equals(otherType.getId())) {
            // model.equipment.tools for example
            return true;
        }
        return compatibleEquipment.contains(otherType.getId()) &&
            otherType.compatibleEquipment.contains(getId());
    }

    /**
     * Returns true if Equipment of this type grants an offence bonus
     * or a defence bonus.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isMilitaryEquipment() {
        return militaryEquipment;
    }

    /**
     * Set the <code>MilitaryEquipment</code> value.
     *
     * @param newMilitaryEquipment The new MilitaryEquipment value.
     */
    public final void setMilitaryEquipment(final boolean newMilitaryEquipment) {
        this.militaryEquipment = newMilitaryEquipment;
    }


    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        maximumCount = getAttribute(in, "maximum-count", 1);
        combatLossPriority = getAttribute(in, "combat-loss-priority", 0);
        String roleString = getAttribute(in, "role", "default");
        role = Enum.valueOf(Role.class, roleString.toUpperCase());
    }

    public void readChildren(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("required-location-ability".equals(nodeName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                getLocationAbilitiesRequired().put(abilityId, value);
                in.nextTag(); // close this element
                specification.getAbilityKeys().add(abilityId);
            } else if ("compatible-equipment".equals(nodeName)) {
                String equipmentId = in.getAttributeValue(null, "id");
                compatibleEquipment.add(equipmentId);
                in.nextTag(); // close this element
            } else {
                super.readChild(in, specification);
            }
        }

        if (militaryEquipment) {
            for (AbstractGoods goods : getGoodsRequired()) {
                goods.getType().setMilitaryGoods(true);
            }
        }

    }
}
