package net.sf.freecol.server.ai.goal;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;

import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;

public class GotoAdjacentGoal extends Goal {
    private static final Logger logger = Logger.getLogger(GotoAdjacentGoal.class.getName());

    //the destination tile
    private Tile target;

    public GotoAdjacentGoal(AIPlayer p, Goal g, float w, AIUnit u, Tile t) {
        super(p,g,w,u);
        target = t;
    }

    protected Iterator<AIUnit> getOwnedAIUnitsIterator() {
        //we're keeping units on the availableUnitsList,
        //so all our own units at any moment are these
        return availableUnitsList.iterator();
    }

    protected Iterator<Goal> getSubGoalIterator() {
        return null;
    }
    
    protected void removeUnit(AIUnit u) {
        Iterator<AIUnit> uit = availableUnitsList.iterator();
        while (uit.hasNext()) {
            AIUnit unit = uit.next();
            if (unit.equals(u)) {
                uit.remove();
            }
        }
    }
    
    protected void plan() {
        isFinished = false;
        
        //Run through available units. For each unit, find a path to the
        //target and move towards it. Return to parent if adjacent to target,
        //or no path can be found.
        Iterator<AIUnit> uit = availableUnitsList.iterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();

            PathNode pathNode = u.getUnit().findPath(target);
            if (pathNode==null) {
                uit.remove();
                addUnitToParent(u);            
            } else {
                while (pathNode.next != null 
                        && pathNode.getTurns() == 0
                        && pathNode.getTile() != target
                        && (u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.MOVE
                          ||u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.EXPLORE_LOST_CITY_RUMOUR)) {
                        
                            if(u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.EXPLORE_LOST_CITY_RUMOUR) {
                                logger.warning("Accidental rumour exploration!");
                            }
                        
                            Element moveElement = Message.createNewRootElement("move");
                            moveElement.setAttribute("unit", u.getUnit().getId());
                            moveElement.setAttribute("direction", pathNode.getDirection().toString());

                            try {
                                player.getConnection().sendAndWait(moveElement);
                            } catch (IOException e) {
                                logger.warning("Could not send \"move\"-message!");
                            }
                            
                            pathNode = pathNode.next;
                }
                if (u.getUnit().getTile().isAdjacent(target)) {
                    //If unit is adjacent after moving, return to parent
                    uit.remove();
                    addUnitToParent(u);
                }
            }
        }
        
        if (availableUnitsList.size()==0) {
            //we don't have any units left to deal with,
            //signal that we may safely be cancelled now
            isFinished = true;
        }
    }

    public String getGoalDescription() {
        String descr = super.getGoalDescription();
        if (target!=null) {
            descr += ":"+target.getX()+","+target.getY();
        } else {
            descr += ":null";
        }
        return descr;
    }
    
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
    }
    
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
    }
}