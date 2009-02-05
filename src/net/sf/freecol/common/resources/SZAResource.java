/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.common.resources;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.io.sza.SimpleZippedAnimation;

/**
 * A <code>Resource</code> wrapping a <code>SimpleZippedAnimation</code>.
 * 
 * @see Resource
 * @see SimpleZippedAnimation
 */
public class SZAResource extends Resource {
    private static final Logger logger = Logger.getLogger(SZAResource.class.getName());

    private Map<Double, SimpleZippedAnimation> scaledSzAnimations = new HashMap<Double, SimpleZippedAnimation>();
    private SimpleZippedAnimation szAnimation = null;
    private volatile Object loadingLock = new Object();
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URL</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URL)
     */
    SZAResource(URL resourceLocator) {
        super(resourceLocator);
    }
    
    
    /**
     * Gets the <code>SimpleZippedAnimation</code> represented by this resource.
     * @return The <code>SimpleZippedAnimation</code> in it's original size.
     */
    public SimpleZippedAnimation getSimpleZippedAnimation() {
        if (szAnimation != null) {
            return szAnimation;
        }
        synchronized (loadingLock) {
            if (szAnimation != null) {
                return szAnimation;
            }
            try {
                szAnimation = new SimpleZippedAnimation(getResourceLocator());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not load SimpleZippedAnimation: " + getResourceLocator(), e);
            }
            return szAnimation;
        }
    }
    
    /**
     * Returns the <code>SimpleZippedAnimation</code> using the specified scale.
     * 
     * @param scale The size of the requested animation (with 1 being normal
     *      size, 2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The <code>SimpleZippedAnimation</code>.
     */
    public SimpleZippedAnimation getSimpleZippedAnimation(double scale) {
        final SimpleZippedAnimation sza = getSimpleZippedAnimation();
        if (scale == 1.0) {
            return sza;
        }
        final SimpleZippedAnimation cachedScaledVersion = scaledSzAnimations.get(scale);
        if (cachedScaledVersion != null) {
            return cachedScaledVersion;
        }
        synchronized (loadingLock) {
            if (scaledSzAnimations.get(scale) != null) {
                return scaledSzAnimations.get(scale);
            }
            final SimpleZippedAnimation scaledVersion = sza.createScaledVersion(scale);
            scaledSzAnimations.put(scale, scaledVersion);
            return scaledVersion;
        }
    }
}
