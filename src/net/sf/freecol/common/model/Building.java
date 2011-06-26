/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * Represents a building in a colony.
 */
public class Building extends FreeColGameObject
    implements WorkLocation, Ownable, Named, Comparable<Building>, Consumer {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Building.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The colony containing this building. */
    protected Colony colony;

    /** The type of building. */
    protected BuildingType buildingType;

    /**
     * List of the units which have this <code>Building</code> as it's
     * {@link Unit#getLocation() location}.
     */
    private final List<Unit> units = new ArrayList<Unit>();


    /**
     * Constructor for ServerBuilding.
     */
    protected Building() {
        // empty constructor
    }

    /**
     * Constructor for ServerBuilding.
     *
     * @param game The <code>Game</code> this object belongs to.
     */
    protected Building(Game game) {
        super(game);
    }

    /**
     * Initiates a new <code>Building</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Building(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initiates a new <code>Building</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Building(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Building</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Building(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the owner of this <code>Ownable</code>.
     *
     * @return The <code>Player</code> controlling this {@link Ownable}.
     */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership of this
     *            {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by this method.
     */
    public void setOwner(final Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the <code>Tile</code> where this <code>Building</code> is
     * located.
     *
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return colony.getTile();
    }

    public String getNameKey() {
        return buildingType.getNameKey();
    }

    /**
     * Returns the level of this building.
     *
     * @return an <code>int</code> value
     */
    public int getLevel() {
        return buildingType.getLevel();
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.template("inLocation")
            .add("%location%", getNameKey());
    }

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to prepare the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
    }

    /**
     * Gets the name of the improved building of the same type. An improved
     * building is a building of a higher level.
     *
     * @return The name of the improved building or <code>null</code> if the
     *         improvement does not exist.
     */
    public String getNextNameKey() {
        final BuildingType next = buildingType.getUpgradesTo();
        return next == null ? null : next.getNameKey();
    }

    /**
     * Checks if this building can have a higher level.
     *
     * @return If this <code>Building</code> can have a higher level, that
     *         {@link FoundingFather Adam Smith} is present for manufactoring
     *         factory level buildings and that the <code>Colony</code>
     *         containing this <code>Building</code> has a sufficiently high
     *         population.
     */
    public boolean canBuildNext() {
        return getColony().canBuild(buildingType.getUpgradesTo());
    }


    /**
     * Gets a pointer to the settlement containing this building.
     *
     * @return This colony.
     */
    public Settlement getSettlement() {
        return colony;
    }

    /**
     * Gets a pointer to the colony containing this building.
     *
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Gets the type of this building.
     *
     * @return The type.
     */
    public BuildingType getType() {
        return buildingType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAbility(String id) {
        return getType().hasAbility(id);
    }

    /**
     * Returns <code>true</code> if this Building has the Ability to
     * teach skills.
     *
     * @see Ability#CAN_TEACH
     */
    public boolean canTeach() {
        return hasAbility(Ability.CAN_TEACH);
    }

    /**
     * Returns whether this building can be damaged
     *
     * @return <code>true</code> if can be damaged
     * @see #damage
     */
    public boolean canBeDamaged() {
        return !buildingType.isAutomaticBuild()
            && !colony.isAutomaticBuild(buildingType);
    }

    /**
     * Reduces this building to previous level (is set to UpgradesFrom
     * attribute in BuildingType) or is destroyed if it's the first level
     *
     * @return True if the building was damaged.
     */
    public boolean damage() {
        if (canBeDamaged()) {
            setType(buildingType.getUpgradesFrom());
            return true;
        }
        return false;
    }

    /**
     * Upgrades this building to next level (is set to UpgradesTo
     * attribute in BuildingType)
     *
     * @return True if the upgrade succeeds.
     */
    public boolean upgrade() {
        if (canBuildNext()) {
            setType(buildingType.getUpgradesTo());
            return true;
        }
        return false;
    }

    /**
     * Changes the type of the Building. The type of a building may
     * change when it is upgraded or damaged.
     *
     * @param newBuildingType
     * @see #upgrade
     * @see #damage
     */
    private void setType(final BuildingType newBuildingType) {
        // remove features from current type
        colony.getFeatureContainer().remove(buildingType.getFeatureContainer());

        if (newBuildingType != null) {
            buildingType = newBuildingType;

            // add new features and abilities from new type
            colony.getFeatureContainer().add(buildingType.getFeatureContainer());

            // Colonists which can't work here must be put outside
            for (Unit unit : units) {
                if (!canAdd(unit.getType())) {
                    unit.putOutsideColony();
                }
            }
        }

        // Colonists exceding units limit must be put outside
        if (units.size() > getMaxUnits()) {
            for (Unit unit : new ArrayList<Unit>(units.subList(getMaxUnits(), units.size()))) {
                unit.putOutsideColony();
            }
        }
    }

    /**
     * Gets the maximum number of units allowed in this <code>Building</code>.
     *
     * @return The number.
     */
    public int getMaxUnits() {
        return buildingType.getWorkPlaces();
    }

    /**
     * Gets the amount of units at this <code>WorkLocation</code>.
     *
     * @return The amount of units at this {@link WorkLocation}.
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * Returns the unit type being an expert in this <code>Building</code>.
     *
     * @return The UnitType.
     */
    public UnitType getExpertUnitType() {
        return getSpecification().getExpertForProducing(getGoodsOutputType());
    }

    /**
     * Checks if the specified <code>Locatable</code> may be added to this
     * <code>WorkLocation</code>.
     *
     * @param locatable the <code>Locatable</code>.
     * @return <i>true</i> if the <i>Unit</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(final Locatable locatable) {
        if (locatable.getLocation() == this) {
            return true;
        }

        if (getUnitCount() >= getMaxUnits()) {
            return false;
        }

        if (!(locatable instanceof Unit)) {
            return false;
        }
        return canAdd(((Unit) locatable).getType());
    }

    /**
     * Checks if the specified <code>UnitType</code> may be added to this
     * <code>WorkLocation</code>.
     *
     * @param unitType the <code>UnitType</code>.
     * @return <i>true</i> if the <i>UnitType</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(final UnitType unitType) {
        return buildingType.canAdd(unitType);
    }


    /**
     * Adds the specified locatable to this building.
     *
     * @param locatable The <code>Locatable</code> to add.
     */
    public void add(final Locatable locatable) {
        if (!canAdd(locatable)) {
            throw new IllegalStateException("Can not add " + locatable
                                            + " to " + toString());
        }
        if (units.contains(locatable)) return;

        final Unit unit = (Unit) locatable;
        units.add(unit);
        unit.setState(Unit.UnitState.IN_COLONY);
        unit.setWorkType(getGoodsOutputType());
        getColony().invalidateCache();

        if (canTeach()) {
            Unit student = unit.getStudent();
            if (student == null
                && (student = getColony().findStudent(unit)) != null) {
                unit.setStudent(student);
                student.setTeacher(unit);
            }
            unit.setWorkType(null);
        } else {
            Unit teacher = unit.getTeacher();
            if (teacher == null
                && (teacher = getColony().findTeacher(unit)) != null) {
                unit.setTeacher(teacher);
                teacher.setStudent(unit);
            }
        }
    }

    /**
     * Removes the specified locatable from this building.
     *
     * @param locatable The <code>Locatable</code> to remove.
     */
    public void remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Can only remove units from building.");
        }

        final Unit unit = (Unit) locatable;
        if (units.remove(unit)) {
            unit.setMovesLeft(0);
            unit.setState(Unit.UnitState.ACTIVE);
            getColony().invalidateCache();

            if (canTeach()) {
                Unit student = unit.getStudent();
                if (student != null) {
                    student.setTeacher(null);
                    unit.setStudent(null);
                }
            } else {
                Unit teacher = unit.getTeacher();
                if (teacher != null) {
                    teacher.setStudent(null);
                    unit.setTeacher(null);
                }
            }
        }
    }

    /**
     * Checks if this <code>Building</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><code>true</code>if the specified
     *            <code>Locatable</code> is in this <code>Building</code>
     *            and</li>
     *            <li><code>false</code> otherwise.</li>
     *            </ul>
     */
    public boolean contains(final Locatable locatable) {
        return units.contains(locatable);
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Building</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return units.iterator();
    }

    /**
     * Returns a new <code>List</code> containing all the Units
     * present in the Building.
     *
     * @return a new <code>List</code> containing all the Units
     * present in the Building.
     */
    public List<Unit> getUnitList() {
        return new ArrayList<Unit>(units);
    }

    /**
     * Returns this <code>Building</code>'s
     * <code>GoodsContainer</code>, which is always <code>null</code>.
     *
     * @return <code>null</code>.
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Returns the type of goods this <code>Building</code> produces,
     * or <code>null</code> if the Building does not produce any
     * goods.
     *
     * @return The type of goods this <code>Building</code> produces
     */
    public GoodsType getGoodsOutputType() {
        return getType().getProducedGoodsType();
    }

    /**
     * Returns the type of goods this building needs for input, or
     * <code>null</code> if the Building does not consume any goods.
     *
     * @return The type of goods this <code>Building</code> requires as input
     *         in order to produce it's {@link #getGoodsOutputType output}.
     */
    public GoodsType getGoodsInputType() {
        return getType().getConsumedGoodsType();
    }

    /**
     * Returns the ProductionInfo for this Building from the Colony's
     * cache.
     *
     * @return a <code>ProductionInfo</code> object
     */
    public ProductionInfo getProductionInfo() {
        return colony.getProductionInfo(this);
    }

    /**
     * Returns the ProductionInfo for this Building.
     *
     * @param output the output goods already available in the colony,
     *        necessary in order to avoid excess production
     * @param input the input goods available
     * @return a <code>ProductionInfo</code> object
     */
    public ProductionInfo getProductionInfo(AbstractGoods output, List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        GoodsType outputType = getGoodsOutputType();
        GoodsType inputType = getGoodsInputType();
        if (outputType != null && outputType != output.getType()) {
            throw new IllegalArgumentException("Wrong output type: " + output.getType()
                                               + " should have been: " + outputType);
        }
        int capacity = colony.getWarehouseCapacity();
        if (buildingType.hasAbility(Ability.AVOID_EXCESS_PRODUCTION)
            && output.getAmount() >= capacity) {
            // warehouse is already full: produce nothing
            return result;
        }

        int availableInput = 0;
        if (inputType != null) {
            boolean found = false;
            for (AbstractGoods goods : input) {
                if (goods.getType() == inputType) {
                    availableInput = goods.getAmount();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("No input goods of type "
                                                   + inputType + " available.");
            }
        }

        if (outputType != null) {
            int maximumInput = 0;
            if (inputType != null && canAutoProduce()) {
                int available = colony.getGoodsCount(outputType);
                if (available >= outputType.getBreedingNumber()) {
                    // we need at least these many horses/animals to breed
                    int divisor = (int) getType().getFeatureContainer()
                        .applyModifier(0, "model.modifier.breedingDivisor");
                    int factor = (int) getType().getFeatureContainer()
                        .applyModifier(0, "model.modifier.breedingFactor");
                    maximumInput = ((available - 1) / divisor + 1) * factor;
                }
            } else {
                maximumInput = getProductivity();
            }
            List<Modifier> productionModifiers = getProductionModifiers();
            int maximumProduction = (int) FeatureContainer
                .applyModifiers(maximumInput, getGame().getTurn(),
                                productionModifiers);
            int actualInput = (inputType == null)
                ? maximumInput
                : Math.min(maximumInput, availableInput);
            // experts in factory level buildings may produce a
            // certain amount of goods even when no input is available
            if (availableInput < maximumInput
                && buildingType.hasAbility(Ability.EXPERTS_USE_CONNECTIONS)
                && getSpecification().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                int minimumGoodsInput = 0;
                for (Unit unit: units) {
                    if (unit.getType() == getExpertUnitType()) {
                        // TODO: put magic number in specification
                        minimumGoodsInput += 4;
                    }
                }
                if (minimumGoodsInput > availableInput) {
                    actualInput = minimumGoodsInput;
                }
            }
            // output is the same as input, plus production bonuses
            int production = (int) FeatureContainer
                .applyModifiers(actualInput, getGame().getTurn(),
                                productionModifiers);
            if (production > 0) {
                if (buildingType.hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    int total = output.getAmount() + production;
                    while (total > capacity) {
                        if (actualInput <= 0) {
                            // produce nothing
                            return result;
                        } else {
                            actualInput--;
                        }
                        production = (int) FeatureContainer
                            .applyModifiers(actualInput, getGame().getTurn(),
                                            productionModifiers);
                        total = output.getAmount() + production;
                        // in this case, maximum production does not
                        // exceed actual production
                        maximumInput = actualInput;
                        maximumProduction = production;
                    }
                }
                result.addProduction(new AbstractGoods(outputType, production));
                if (maximumProduction > production) {
                    result.addMaximumProduction(new AbstractGoods(outputType, maximumProduction));
                }
                if (inputType != null) {
                    result.addConsumption(new AbstractGoods(inputType, actualInput));
                    if (maximumInput > actualInput) {
                        result.addMaximumConsumption(new AbstractGoods(inputType, maximumInput));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the actual production of this building.
     *
     * @return The amount of goods being produced by this <code>Building</code>
     *         the current turn. The type of goods being produced is given by
     *         {@link #getGoodsOutputType}.
     * @see #getMaximumProduction
     */
    public int getProduction() {
        List<AbstractGoods> production = getProductionInfo().getProduction();
        if (production == null || production.isEmpty()) {
            return 0;
        } else {
            return production.get(0).getAmount();
        }
    }

    /**
     * Returns the maximum production of this building.
     *
     * @return The production of this building, with the current amount of
     *         workers, when there is enough "input goods".
     */
    public int getMaximumProduction() {
        List<AbstractGoods> production = getProductionInfo().getMaximumProduction();
        if (production == null || production.isEmpty()) {
            return getProduction();
        } else {
            return production.get(0).getAmount();
        }
    }

    /**
     * Returns the additional production of new <code>Unit</code> at
     * this building for next turn.
     *
     * TODO: Make this work properly. In the past, the method never
     * worked correctly anyway, since it did not take the possible
     * decrease in the production of input goods into account that
     * might be caused by moving a unit to this Building from another
     * WorkLocation. To do this right, it would be necessary to
     * re-calculate the production for the whole colony.
     *
     * @return The production of this building the next turn.
     */
    public int getAdditionalProductionNextTurn(Unit addUnit) {
        return getUnitProductivity(addUnit);
    }

    /**
     * Returns true if this building can produce goods without workers.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canAutoProduce() {
        return buildingType.hasAbility(Ability.AUTO_PRODUCTION);
    }

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production for.
     * @return the production og the given goods this turn. This method will
     *         return the same as {@link #getProduction} if the given type of
     *         goods is the same as {@link #getGoodsOutputType} and
     *         <code>0</code> otherwise.
     */
    public int getProductionOf(GoodsType goodsType) {
        if (goodsType == getGoodsOutputType()) {
            return getProduction();
        }

        return 0;
    }

    /**
     * Returns the maximum productivity of worker/s currently working
     * in this building.
     *
     * @param additionalUnits units to add before calculating result
     * @return The maximum returns from workers in this building,
     *         assuming enough "input goods".
     */
    private int getProductivity(Unit... additionalUnits) {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int productivity = 0;
        for (Unit unit : units) {
            productivity += getUnitProductivity(unit);
        }
        for (Unit unit : additionalUnits) {
            if (canAdd(unit)) {
                productivity += getUnitProductivity(unit);
            }
        }
        return productivity;
    }

    /**
     * Returns the maximum productivity of a unit working in this building.
     *
     * @return The maximum returns from this unit if in this <code>Building</code>,
     *         assuming enough "input goods".
     */
    public int getUnitProductivity(Unit prodUnit) {
        if (getGoodsOutputType() == null || prodUnit == null) {
            return 0;
        }

        int productivity = buildingType.getBasicProduction();
        if (productivity > 0) {
            productivity += colony.getProductionBonus();
            return (int) prodUnit.getType().getFeatureContainer()
                .applyModifier(Math.max(1, productivity),
                               getGoodsOutputType().getId());
        } else {
            return 0;
        }
    }


    /**
     * Returns a List of all Modifiers that influence the total
     * production of the Building. In particular, the method does not
     * return any Modifiers that influence of the productivity of
     * individual units present in the Building, such as the colony
     * production bonus.
     *
     * @return a List of all Modifiers that influence the total
     * production of the Building
     */
    public List<Modifier> getProductionModifiers() {
        List<Modifier> modifiers = new ArrayList<Modifier>();
        GoodsType goodsOutputType = getGoodsOutputType();
        if (goodsOutputType != null) {
            modifiers.addAll(colony.getFeatureContainer().
                             getModifierSet(goodsOutputType.getId(), buildingType, getGame().getTurn()));
            Collections.sort(modifiers);
        }
        return modifiers;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Building other) {
        return getType().compareTo(other.getType());
    }


    /**
     * Dispose of this building.
     *
     * @return A list of disposed objects.
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        while (!units.isEmpty()) {
            objects.addAll(units.remove(0).disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Disposes this building. All units that currently has this
     * <code>Building as it's location will be disposed</code>.
     */
    @Override
    public void dispose() {
        disposeList();
    }


    // Interface Consumer

    /**
     * Returns true if this Consumer consumes the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType) {
        return goodsType == getGoodsInputType();
    }

    /**
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        GoodsType inputType = getGoodsInputType();
        if (inputType != null) {
            result.add(new AbstractGoods(inputType, 0));
        }
        return result;
    }

    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        return buildingType.getPriority();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Modifier> getModifierSet(String id) {
        return buildingType.getModifierSet(id);
    }

    // Serialization

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * <br>
     * <br>
     *
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("colony", colony.getId());
        out.writeAttribute("buildingType", buildingType.getId());

        // Add child elements:
        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next().toXML(out, player, showAll, toSavedGame);
        }

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        colony = getFreeColGameObject(in, "colony", Colony.class);
        buildingType = getSpecification().getBuildingType(in.getAttributeValue(null, "buildingType"));

        units.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            Unit unit = updateFreeColGameObject(in, Unit.class);
            if (!units.contains(unit)) units.add(unit);
        }
    }

    /**
     * Partial writer, so that "remove" messages can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * String converter for debugging.
     *
     * @return The name of the building.
     */
    public String toString() {
        return getType().getId() + " [" + colony.getName() + "]";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "building";
    }
}
