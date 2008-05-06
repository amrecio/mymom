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

package net.sf.freecol.client.gui.plaf;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextAreaUI;

import net.sf.freecol.common.resources.ResourceManager;

/**
* Draws the image "BackgroundImage2" from the defaults table as a tiled
* background image.
*/
public class FreeColTextAreaUI extends BasicTextAreaUI {

    private JComponent c;

    public FreeColTextAreaUI(JComponent c) {
        this.c = c;
    }
    
    public static ComponentUI createUI(JComponent c) {
        return new FreeColTextAreaUI(c);
    }

    @Override
    public void paintSafely(Graphics g) { 
        LAFUtilities.setProperties(g, c);
        super.paintSafely(g);
    }
    
    public void paintBackground(java.awt.Graphics g) {
        JComponent c = getComponent();

        if (c.isOpaque()) {
            int width = c.getWidth();
            int height = c.getHeight();

            Image tempImage = ResourceManager.getImage("BackgroundImage2");

            if (tempImage != null) {
                for (int x=0; x<width; x+=tempImage.getWidth(null)) {
                    for (int y=0; y<height; y+=tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(c.getBackground());
                g.fillRect(0, 0, width, height);
            }
        }

    }

}
