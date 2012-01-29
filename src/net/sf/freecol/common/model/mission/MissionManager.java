/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model.mission;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Game;

/**
 * The MissionManager keeps track of all missions defined.
 *
 * @see net.sf.freecol.client.gui.action.ActionManager
 */
public class MissionManager {

    private static final Logger logger = Logger.getLogger(MissionManager.class.getName());

    private static Map<String, Constructor<? extends Mission>> missionMap =
        new HashMap<String, Constructor<? extends Mission>>();

    static {
        try {
            missionMap.put(CompoundMission.getXMLElementTagName(),
                           CompoundMission.class.getConstructor(Game.class, XMLStreamReader.class));
            missionMap.put(GoToMission.getXMLElementTagName(),
                           GoToMission.class.getConstructor(Game.class, XMLStreamReader.class));
            missionMap.put(ImprovementMission.getXMLElementTagName(),
                           ImprovementMission.class.getConstructor(Game.class, XMLStreamReader.class));

        } catch (NoSuchMethodException e) {
            logger.warning("Missing constructor: " + e.toString());
        }
    }


    /**
     * Returns true if the given String is a known mission tag.
     *
     * @param tag a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isMissionTag(String tag) {
        return missionMap.containsKey(tag);
    }

    /**
     * Returns a new Mission read from the input stream if possible,
     * and null if not.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @return a <code>Mission</code> value
     * @exception XMLStreamException if an error occurs
     */
    public static Mission getMission(Game game, XMLStreamReader in)
        throws XMLStreamException {
        String tag = in.getLocalName();
        Constructor<? extends Mission> c = missionMap.get(tag);
        if (c == null) {
            logger.warning("Unknown type of mission: '" + tag + "'.");
            in.nextTag();
            return null;
        } else {
            try {
                return c.newInstance(game, in);
            } catch(Exception e) {
                logger.warning("Failed to instatiate mission with tag '" + tag + "'.");
                return null;
            }
        }
    }


}