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
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.UnitTypeChange.ChangeType;

public final class UnitType extends BuildableType implements Comparable<UnitType> {

    public static int nextIndex = 0;

    public static final int DEFAULT_OFFENCE = 0;
    public static final int DEFAULT_DEFENCE = 1;

    /**
     * Describe offence here.
     */
    private int offence = DEFAULT_OFFENCE;

    /**
     * Describe defence here.
     */
    private int defence = DEFAULT_DEFENCE;

    /**
     * Describe space here.
     */
    private int space = 0;

    /**
     * Describe hitPoints here.
     */
    private int hitPoints = 0;

    /**
     * Describe spaceTaken here.
     */
    private int spaceTaken = 1;

    /**
     * Describe skill here.
     */
    private int skill = UNDEFINED;

    /**
     * Describe price here.
     */
    private int price = UNDEFINED;

    /**
     * Describe movement here.
     */
    private int movement = 3;
    
    /**
     * Describe lineOfSight here.
     */
    private int lineOfSight = 1;

    /**
     * Describe recruitProbability here.
     */
    private int recruitProbability = 0;

    /**
     * Describe expertProduction here.
     */
    private GoodsType expertProduction;

    /**
     * How much a Unit of this type contributes to the Player's score.
     */
    private int scoreValue = 0;

    /**
     * Describe maximumAttrition here.
     */
    private int maximumAttrition = INFINITY;

    /**
     * The skill this UnitType teaches, mostly its own.
     */
    private UnitType skillTaught;

    /**
     * Describe defaultEquipment here.
     */
    private EquipmentType defaultEquipment;

    /**
     * The goods consumed per turn when in a settlement.
     */
    private TypeCountMap<GoodsType> consumption = new TypeCountMap<GoodsType>();

    /**
     * The possible type changes for this unit type.
     */
    private List<UnitTypeChange> typeChanges = new ArrayList<UnitTypeChange>();
    

    /**
     * Creates a new <code>UnitType</code> instance.
     *
     */
    public UnitType() {
        setIndex(nextIndex++);
        setModifierIndex(Modifier.EXPERT_PRODUCTION_INDEX);
    }

    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Returns <code>true</code> if Units of this type can carry other Units.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryUnits() {
        return hasAbility("model.ability.carryUnits");
    }

    /**
     * Returns <code>true</code> if Units of this type can carry Goods.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryGoods() {
        return hasAbility("model.ability.carryGoods");
    }

    /**
     * Get the <code>ScoreValue</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the <code>ScoreValue</code> value.
     *
     * @param newScoreValue The new ScoreValue value.
     */
    public void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }

    /**
     * Get the <code>Offence</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getOffence() {
        return offence;
    }

    /**
     * Set the <code>Offence</code> value.
     *
     * @param newOffence The new Offence value.
     */
    public void setOffence(final int newOffence) {
        this.offence = newOffence;
    }

    /**
     * Get the <code>Defence</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getDefence() {
        return defence;
    }

    /**
     * Set the <code>Defence</code> value.
     *
     * @param newDefence The new Defence value.
     */
    public void setDefence(final int newDefence) {
        this.defence = newDefence;
    }

    /**
     * Get the <code>LineOfSight</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getLineOfSight() {
        return lineOfSight;
    }

    /**
     * Set the <code>LineOfSight</code> value.
     *
     * @param newLineOfSight The new Defence value.
     */
    public void setLineOfSight(final int newLineOfSight) {
        this.lineOfSight = newLineOfSight;
    }

    /**
     * Get the <code>Space</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSpace() {
        return space;
    }

    /**
     * Set the <code>Space</code> value.
     *
     * @param newSpace The new Space value.
     */
    public void setSpace(final int newSpace) {
        this.space = newSpace;
    }

    /**
     * Get the <code>HitPoints</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * Set the <code>HitPoints</code> value.
     *
     * @param newHitPoints The new HitPoints value.
     */
    public void setHitPoints(final int newHitPoints) {
        this.hitPoints = newHitPoints;
    }

    /**
     * Get the <code>SpaceTaken</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSpaceTaken() {
        return Math.max(spaceTaken, space + 1);
    }

    /**
     * Set the <code>SpaceTaken</code> value.
     *
     * @param newSpaceTaken The new SpaceTaken value.
     */
    public void setSpaceTaken(final int newSpaceTaken) {
        this.spaceTaken = newSpaceTaken;
    }

    /**
     * If this UnitType is recruitable in Europe
     *
     * @return an <code>boolean</code> value
     */
    public boolean isRecruitable() {
        return recruitProbability > 0;
    }

    /**
     * Get the <code>RecruitProbability</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getRecruitProbability() {
        return recruitProbability;
    }

    /**
     * Set the <code>RecruitProbability</code> value.
     *
     * @param newRecruitProbability The new RecruitProbability value.
     */
    public void setRecruitProbability(final int newRecruitProbability) {
        this.recruitProbability = newRecruitProbability;
    }

    /**
     * Get the <code>Skill</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSkill() {
        return skill;
    }

    /**
     * Set the <code>Skill</code> value.
     *
     * @param newSkill The new Skill value.
     */
    public void setSkill(final int newSkill) {
        this.skill = newSkill;
    }

    /**
     * Get the <code>Price</code> value.
     *
     * @return an <code>int</code> value
     * 
     * This returns the base price of the <code>UnitType</code>
     * 
     * For the actual price of the unit, use {@link Europe#getUnitPrice(UnitType)} 
     */
    public int getPrice() {
        return price;
    }

    /**
     * Set the <code>Price</code> value.
     *
     * @param newPrice The new Price value.
     */
    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }

    /**
     * Get the <code>Movement</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMovement() {
        return movement;
    }

    /**
     * Set the <code>Movement</code> value.
     *
     * @param newMovement The new Movement value.
     */
    public void setMovement(final int newMovement) {
        this.movement = newMovement;
    }

    /**
     * Get the <code>MaximumAttrition</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMaximumAttrition() {
        return maximumAttrition;
    }

    /**
     * Set the <code>MaximumAttrition</code> value.
     *
     * @param newMaximumAttrition The new MaximumAttrition value.
     */
    public void setMaximumAttrition(final int newMaximumAttrition) {
        this.maximumAttrition = newMaximumAttrition;
    }

    /**
     * Get the <code>ExpertProduction</code> value.
     *
     * @return a <code>GoodsType</code> value
     */
    public GoodsType getExpertProduction() {
        return expertProduction;
    }

    /**
     * Set the <code>ExpertProduction</code> value.
     *
     * @param newExpertProduction The new ExpertProduction value.
     */
    public void setExpertProduction(final GoodsType newExpertProduction) {
        this.expertProduction = newExpertProduction;
    }

    /**
     * Get the <code>DefaultEquipment</code> value.
     *
     * @return an <code>EquipmentType</code> value
     */
    public EquipmentType getDefaultEquipmentType() {
        return defaultEquipment;
    }

    /**
     * Set the <code>DefaultEquipment</code> value.
     *
     * @param newDefaultEquipment The new DefaultEquipment value.
     */
    public void setDefaultEquipmentType(final EquipmentType newDefaultEquipment) {
        this.defaultEquipment = newDefaultEquipment;
    }

    public EquipmentType[] getDefaultEquipment() {
        if (hasAbility("model.ability.canBeEquipped") && defaultEquipment != null) {
            int count = defaultEquipment.getMaximumCount();
            EquipmentType[] result = new EquipmentType[count];
            for (int index = 0; index < count; index++) {
                result[index] = defaultEquipment;
            }
            return result;
        } else {
            return EquipmentType.NO_EQUIPMENT;
        }
    }

    public List<UnitTypeChange> getTypeChanges() {
        return typeChanges;
    }

    /**
     * Get the <code>SkillTaught</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public UnitType getSkillTaught() {
        return skillTaught;
    }

    /**
     * Set the <code>SkillTaught</code> value.
     *
     * @param newSkillTaught The new SkillTaught value.
     */
    public void setSkillTaught(final UnitType newSkillTaught) {
        this.skillTaught = newSkillTaught;
    }

    /**
     * Returns the number of units of the given GoodsType this
     * UnitType consumes per turn (when in a settlement).
     *
     * @return units consumed
     */
    public int getConsumptionOf(GoodsType goodsType) {
        return consumption.getCount(goodsType);
    }

    public int compareTo(UnitType other) {
        return getIndex() - other.getIndex();
    }

    /**
     * Returns true if the UnitType is available to the given
     * Player.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAvailableTo(Player player) {
        java.util.Map<String, Boolean> requiredAbilities = getAbilitiesRequired();
        for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
            if (player.hasAbility(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Describe <code>getUnitTypeChange</code> method here.
     *
     * @param changeType an <code>UnitTypeChange.Type</code> value
     * @param player a <code>Player</code> value
     * @return an <code>UnitType</code> value
     */
    public UnitType getUnitTypeChange(ChangeType changeType, Player player) {
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(changeType) && change.appliesTo(player)) {
                UnitType result = change.getNewUnitType();
                if (result.isAvailableTo(player)) {
                    return result;
                }
            }
        }
        return null;
    }

    

    /**
     * Return true if this UnitType can be upgraded to the given
     * UnitType by the given means of education. If the given UnitType
     * is null, return true if the UnitType can be upgraded to any
     * other UnitType by the given means of education.
     *
     * @param newType the UnitType to learn
     * @param changeType an <code>ChangeType</code> value
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canBeUpgraded(UnitType newType, ChangeType changeType) {
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(changeType)) {
                if (newType == null
                    || newType == change.getNewUnitType()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get a list of UnitType which can learn in a lost city rumour
     *
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public List<UnitType> getUnitTypesLearntInLostCity() {
        List<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(ChangeType.LOST_CITY)) {
                unitTypes.add(change.getNewUnitType());
            }
        }
        return unitTypes;
    }

    /**
     * Get a UnitType to learn with a level skill less or equal than given level
     *
     * @param maximumSkill the maximum level skill which we are searching for
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public UnitType getEducationUnit(int maximumSkill) {
        for (UnitTypeChange change : typeChanges) {
            if (change.canBeTaught()) {
                UnitType unitType = change.getNewUnitType();
                if (unitType.hasSkill() && unitType.getSkill() <= maximumSkill) {
                    return unitType;
                }
            }
        }
        return null;
    }

    /**
     * Get the <code>EducationTurns</code> value.
     *
     * @return a <code>int</code> value
     */
    public int getEducationTurns(UnitType unitType) {
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(UnitTypeChange.ChangeType.EDUCATION)) {
                if (unitType == change.getNewUnitType()) {
                    return change.getTurnsToLearn();
                }
            }
        }
        return UNDEFINED;
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        String extendString = in.getAttributeValue(null, "extends");
        UnitType parent = (extendString == null) ? this :
            specification.getUnitType(extendString);
        offence = getAttribute(in, "offence", parent.offence);
        defence = getAttribute(in, "defence", parent.defence);
        movement = getAttribute(in, "movement", parent.movement);
        lineOfSight = getAttribute(in, "lineOfSight", parent.lineOfSight);
        scoreValue = getAttribute(in, "scoreValue", parent.scoreValue);
        space = getAttribute(in, "space", parent.space);
        hitPoints = getAttribute(in, "hitPoints", parent.hitPoints);
        spaceTaken = getAttribute(in, "spaceTaken", parent.spaceTaken);
        maximumAttrition = getAttribute(in, "maximumAttrition", parent.maximumAttrition);
        String skillString = in.getAttributeValue(null, "skillTaught");
        skillTaught = (skillString == null) ? this : specification.getUnitType(skillString);

        recruitProbability = getAttribute(in, "recruitProbability", parent.recruitProbability);
        skill = getAttribute(in, "skill", parent.skill);

        setPopulationRequired(getAttribute(in, "population-required", parent.getPopulationRequired()));

        price = getAttribute(in, "price", parent.price);

        expertProduction = specification.getType(in, "expert-production", GoodsType.class,
                                                 parent.expertProduction);

        if (parent != this) {
            typeChanges.addAll(parent.typeChanges);
            defaultEquipment = parent.defaultEquipment;
            getFeatureContainer().add(parent.getFeatureContainer());
            consumption.putAll(parent.consumption);
        }
    }

    public void readChildren(XMLStreamReader in, Specification specification) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("downgrade".equals(nodeName)
                || "upgrade".equals(nodeName)) {
                UnitTypeChange change = new UnitTypeChange(in, specification);
                if (change.getNewUnitType() == null) {
                    typeChanges.clear();
                } else {
                    if ("downgrade".equals(nodeName)
                        && change.getChangeTypes().isEmpty()) {
                        // add default downgrade type
                        change.getChangeTypes().add(ChangeType.CLEAR_SKILL);
                    }
                    typeChanges.add(change);
                }
            } else if ("default-equipment".equals(nodeName)) {
                String equipmentString = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                if (equipmentString != null) {
                    defaultEquipment = specification.getEquipmentType(equipmentString);
                }
                in.nextTag(); // close this element
            } else if ("consumes".equals(nodeName)) {
                String typeString = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                String valueString = in.getAttributeValue(null, VALUE_TAG);
                if (typeString != null && valueString != null) {
                    try {
                        GoodsType type = specification.getGoodsType(typeString);
                        int amount = Integer.parseInt(valueString);
                        consumption.incrementCount(type, amount);
                    } catch(Exception e) {
                        logger.warning("Failed to parse integer " + valueString);
                    }
                }
                in.nextTag(); // close this element
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
        out.writeAttribute("offence", Integer.toString(offence));
        out.writeAttribute("defence", Integer.toString(defence));
        out.writeAttribute("movement", Integer.toString(movement));
        out.writeAttribute("lineOfSight", Integer.toString(lineOfSight));
        out.writeAttribute("scoreValue", Integer.toString(scoreValue));
        out.writeAttribute("space", Integer.toString(space));
        out.writeAttribute("spaceTaken", Integer.toString(spaceTaken));
        out.writeAttribute("hitPoints", Integer.toString(hitPoints));
        if (maximumAttrition < INFINITY) {
            out.writeAttribute("maximumAttrition", Integer.toString(maximumAttrition));
        }
        out.writeAttribute("recruitProbability", Integer.toString(recruitProbability));
        if (skill != UNDEFINED) {
            out.writeAttribute("skill", Integer.toString(skill));
        }
        if (price != UNDEFINED) {
            out.writeAttribute("price", Integer.toString(price));
        }
        out.writeAttribute("skillTaught", skillTaught.getId());
        if (expertProduction != null) {
            out.writeAttribute("expert-production", expertProduction.getId());
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        for (UnitTypeChange change : typeChanges) {
            change.toXMLImpl(out);
        }
        if (defaultEquipment != null) {
            out.writeStartElement("default-equipment");
            out.writeAttribute(ID_ATTRIBUTE_TAG, defaultEquipment.getId());
            out.writeEndElement();
        }
        if (!consumption.isEmpty()) {
            for (GoodsType goodsType : consumption.keySet()) {
                out.writeStartElement("consumes");
                out.writeAttribute(ID_ATTRIBUTE_TAG, goodsType.getId());
                out.writeAttribute(VALUE_TAG, Integer.toString(consumption.getCount(goodsType)));
                out.writeEndElement();
            }
        }
    }

    public static String getXMLElementTagName() {
        return "unit-type";
    }


    /**
     * Returns true if this UnitType has a skill.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasSkill() {

        return skill != UNDEFINED;
    }


    /**
     * Returns true if this UnitType can be built.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBeBuilt() {
        return getGoodsRequired().isEmpty() == false;
    }


    /**
     * Returns true if this UnitType has a price.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasPrice() {
        return price != UNDEFINED;
    }

    public int getProductionFor(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        }
        
        base = (int) featureContainer.applyModifier(base, goodsType.getId());
        return Math.max(base, 1);
    }


    
}
