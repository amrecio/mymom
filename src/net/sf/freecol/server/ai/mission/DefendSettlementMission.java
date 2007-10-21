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


package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
* Mission for defending a <code>Settlement</code>.
* @see net.sf.freecol.common.model.Settlement Settlement
*/
public class DefendSettlementMission extends Mission {
    /*
     * TODO: This Mission should later use sub-missions for 
     *       eliminating threats etc.
     */
    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());


    /** The <code>Settlement</code> to be protected. */
    private Settlement settlement;
    
    //private Mission subMission;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param settlement The <code>Settlement</code> to defend.
    * @exception NullPointerException if <code>aiUnit == null</code> or
    *        <code>settlement == null</code>. 
    */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit, Settlement settlement) {
        super(aiMain, aiUnit);

        this.settlement = settlement;
        
        if (settlement == null) {
            logger.warning("settlement == null");
            throw new NullPointerException("settlement == null");
        }        
    }

    /**
     * Creates a new <code>DefendSettlementMission</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public DefendSettlementMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>DefendSettlementMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
     public DefendSettlementMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
         super(aiMain);
         readFromXML(in);
     }
    
    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();
        Map map = unit.getGame().getMap();
        
        if (!isValid()) {
            return;
        }
        
        if (unit.getTile() == null) {
            return;
        }
        
        if (unit.isOffensiveUnit()) {
            Unit bestTarget = null;
            float bestDifference = Float.MIN_VALUE;
            int bestDirection = -1;
            
            int[] directions = map.getRandomDirectionArray();
            for (int i=0; i<Map.NUMBER_OF_DIRECTIONS; i++) {
                int direction = directions[i];
                Tile t = map.getNeighbourOrNull(direction, unit.getTile());
                if (t != null
                        && t.getDefendingUnit(unit) != null
                        && t.getDefendingUnit(unit).getOwner().getStance(unit.getOwner()) == Player.WAR
                        && unit.getMoveType(direction) == Unit.ATTACK) {
                    Unit enemyUnit = t.getDefendingUnit(unit);
                    float enemyAttack = enemyUnit.getOffensePower(unit);
                    float weAttack = unit.getOffensePower(enemyUnit);
                    float enemyDefend = enemyUnit.getDefensePower(unit);
                    float weDefend = unit.getDefensePower(enemyUnit);

                    float difference = weAttack / (weAttack + enemyDefend) - enemyAttack / (enemyAttack + weDefend);                  
                    if (difference > bestDifference) {
                        if (difference > 0 || weAttack > enemyDefend) {
                            bestDifference = difference;
                            bestTarget = enemyUnit;
                            bestDirection = direction;
                        }
                    }
                }
            }
            
            if (bestTarget != null) {
                Element element = Message.createNewRootElement("attack");
                element.setAttribute("unit", unit.getId());
                element.setAttribute("direction", Integer.toString(bestDirection));

                try {
                    connection.ask(element);
                } catch (IOException e) {
                    logger.warning("Could not send message!");
                }               
                return;
            }
        }
            
        if (unit.getTile() != settlement.getTile()) {
            // Move towards the target.
            int r = moveTowards(connection, settlement.getTile());
            if (r >= 0) {
                final int mt = getUnit().getMoveType(r);
                if (mt != Unit.ILLEGAL_MOVE && mt != Unit.ATTACK) {
                    move(connection, r);
                }
            }
        } else {
            if (unit.getState() != Unit.FORTIFIED
                    && unit.getState() != Unit.FORTIFYING
                    && unit.checkSetState(Unit.FORTIFYING)) {
                Element changeStateElement = Message.createNewRootElement("changeState");
                changeStateElement.setAttribute("unit", unit.getId());
                changeStateElement.setAttribute("state", Integer.toString(Unit.FORTIFYING));
                try {
                    logger.log(Level.FINEST, "Sending fortity request...");
                    connection.sendAndWait(changeStateElement);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Couldn't fortify unit!", e);
                }
            }
        }
    }    

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the transport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */    
     public Tile getTransportDestination() {
         if (getUnit().getLocation() instanceof Unit) {
             return settlement.getTile();
         } else if (getUnit().getLocation().getTile() == settlement.getTile()) {
             return null;
         } else if (getUnit().getTile() == null || getUnit().findPath(settlement.getTile()) == null) {
             return settlement.getTile();
         } else {
             return null;
         }
     }


     /**
     * Returns the priority of getting the unit to the
     * transport destination.
     *
     * @return The priority.
     */
     public int getTransportPriority() {
        if (getTransportDestination() != null) {
            return NORMAL_TRANSPORT_PRIORITY + 5;
        } else {
            return 0;
        }
     }
     
     /**
      * Gets the settlement.
      * @return The <code>Settlement</code> to be defended by
      *         this <code>Mission</code>.
      */
     public Settlement getSettlement() {
         return settlement;
     }
     
    /**
     * Checks if this mission is still valid to perform.
     *
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public boolean isValid() {
        return !settlement.isDisposed()
                && settlement.getOwner() == getUnit().getOwner()
                && getUnit().isDefensiveUnit();
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("unit", getUnit().getId());
        out.writeAttribute("settlement", settlement.getId());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));        
        
        settlement = (Settlement) getGame().getFreeColGameObject(in.getAttributeValue(null, "settlement"));
        if (settlement == null) {
            logger.warning("settlement == null");
            throw new NullPointerException("settlement == null");
        }
        
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "defendSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "defendSettlementMission";
    }
    
    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     * 
     * @return The <code>String</code>: 
     *      "(x, y) ColonyName"
     *      where <code>x</code> and <code>y</code> is the
     *      coordinates of the settlement for this mission,
     *      and <code>ColonyName</code> is the name
     *      (if available).
     */
    public String getDebuggingInfo() {
        String name = (settlement instanceof Colony) ? ((Colony) settlement).getName() : "";
        return settlement.getTile().getPosition().toString() + " " + name;
    }
}
