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

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


public class InformationDialog extends FreeColDialog<Boolean> {

    private static Image bgImage = ResourceManager.getImage("InformationDialog.backgroundImage");

    /**
     * Returns an information dialog that shows the given 
     * texts and images, and an "OK" button.
     * 
     * @param canvas The parent Canvas.
     * @param text The text to be displayed in the dialog.
     * @param image The image to be displayed in the dialog.
     */
    public InformationDialog(Canvas canvas, String text, ImageIcon image) {
        this(canvas, new String[] { text }, new ImageIcon[] { image });
    }

    /**
     * Returns an information dialog that shows the given 
     * texts and images, and an "OK" button.
     * 
     * @param parent The parent Canvas.
     * @param texts The texts to be displayed in the dialog.
     * @param images The images to be displayed in the dialog.
     */
    public InformationDialog(Canvas parent, String[] texts, ImageIcon[] images) {
        super(parent);
        setLayout(new MigLayout("wrap 1, insets 200 10 10 10", "[510]", "[242]20[20]"));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        if (images != null) {
            textPanel.setLayout(new MigLayout("wrap 2", "", ""));
        }

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setResponse(Boolean.FALSE);
            }
        });

        if (images == null) {
            for (String text : texts) {
                textPanel.add(getDefaultTextArea(text, 30));
            }
        } else {
            for (int i = 0; i < texts.length; i++) {
                if (images[i] == null) {
                    textPanel.add(getDefaultTextArea(texts[i], 30), "skip");
                } else {
                    textPanel.add(new JLabel(images[i]));
                    textPanel.add(getDefaultTextArea(texts[i], 30));
                }
            }
        }

        JScrollPane scrollPane = new JScrollPane(textPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // correct way to make scroll pane opaque
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        setBorder(null);

        add(scrollPane);
        add(okButton, "tag ok");

    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, this);
        } else {
            int width = getWidth();
            int height = getHeight();
            Image tempImage = ResourceManager.getImage("BackgroundImage");
            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
    }

}