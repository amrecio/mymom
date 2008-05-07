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

package net.sf.freecol.common.networking;

import java.util.HashMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIMain;

public class StatisticsMessage extends Message {
    
    private HashMap<String, Long> memoryStats = null;
    private HashMap<String, Long> gameStats = null;
    private HashMap<String, Long> aiStats = null;

    public StatisticsMessage(Game game, AIMain aiMain) {
        // memory statistics
        memoryStats = game.getMemoryStatistics();
        // game statistics
        gameStats = game.getGameStatistics();
        // AI statistics
        if (aiMain!=null) {
            aiStats = aiMain.getAIStatistics();
        }
    }

    public StatisticsMessage(Element element) {
        readFromXML(element);
    }
    

    /**
     * Get the game statistics.
     *
     * @return a <code>Tile</code> value
     */
    public final HashMap<String, Long> getGameStatistics() {
        return gameStats;
    }

    /**
     * Get the ai statistics.
     *
     * @return a <code>Tile</code> value
     */
    public final HashMap<String, Long> getAIStatistics() {
        return aiStats;
    }
    
    /**
     * Get the free memory.
     *
     * @return a <code>Tile</code> value
     */
    public final HashMap<String, Long> getMemoryStatistics() {
        return memoryStats;
    }

    public void readFromXML(Element element) {
        if (!element.getTagName().equals(getXMLElementTagName())) {
            return;
        }
        Element memoryElement = (Element)element.getElementsByTagName("memoryStatistics").item(0);
        if (memoryElement != null) {
            memoryStats = new HashMap<String, Long>();
            NamedNodeMap atts = (NamedNodeMap)memoryElement.getAttributes();
            for (int i=0; i<atts.getLength(); i++) {
                memoryStats.put(atts.item(i).getNodeName(), new Long(atts.item(i).getNodeValue()));
            }
        }
        Element gameElement = (Element)element.getElementsByTagName("gameStatistics").item(0);
        if (gameElement != null) {
            gameStats = new HashMap<String, Long>();
            NamedNodeMap atts = (NamedNodeMap)gameElement.getAttributes();
            for (int i=0; i<atts.getLength(); i++) {
                gameStats.put(atts.item(i).getNodeName(), new Long(atts.item(i).getNodeValue()));
            }
        }
        Element aiElement = (Element)element.getElementsByTagName("aiStatistics").item(0);
        if (aiElement != null) {
            aiStats = new HashMap<String, Long>();
            NamedNodeMap atts = (NamedNodeMap)aiElement.getAttributes();
            for (int i=0; i<atts.getLength(); i++) {
                aiStats.put(atts.item(i).getNodeName(), new Long(atts.item(i).getNodeValue()));
            }
        }
    }
    
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        // memory statistics
        Element memoryElement = result.getOwnerDocument().createElement("memoryStatistics");
        result.appendChild(memoryElement);
        for (String s : memoryStats.keySet()) {
            memoryElement.setAttribute(s, memoryStats.get(s).toString());
        }
        // game statistics
        Element gameElement = result.getOwnerDocument().createElement("gameStatistics");
        result.appendChild(gameElement);
        for (String s : gameStats.keySet()) {
            gameElement.setAttribute(s, gameStats.get(s).toString());
        }
        // AI statistics
        Element aiElement = result.getOwnerDocument().createElement("aiStatistics");
        result.appendChild(aiElement);
        for (String s : aiStats.keySet()) {
            aiElement.setAttribute(s, aiStats.get(s).toString());
        }
        return result;
    }

    public static String getXMLElementTagName() {
        return "statistics";
    }

}
