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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.BooleanOption;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the Turn Report.
 */
public final class ReportTurnPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportTurnPanel(Canvas parent, ModelMessage... messages) {
        super(parent, Messages.message("menuBar.report.turn"));

        Comparator<ModelMessage> comparator = getCanvas().getClient().getClientOptions().getModelMessageComparator();
        if (comparator != null) {
            Arrays.sort(messages, comparator);
        }

        ClientOptions options = getCanvas().getClient().getClientOptions();
        int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);

        Object source = this;
        ModelMessage.MessageType type = null;
        int headlines = 0;

        // count number of headlines
        for (final ModelMessage message : messages) {
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE && message.getSource() != source) {
                source = message.getSource();
                headlines++;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE && message.getType() != type) {
                type = message.getType();
                headlines++;
            }
        }

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 4, fillx", "", ""));

        source = this;
        type = null;

        int row = 1;
        for (final ModelMessage message : messages) {
            // add headline if necessary
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE && message.getSource() != source) {
                source = message.getSource();
                reportPanel.add(getHeadline(source), "newline 20, skip");
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE && message.getType() != type) {
                type = message.getType();
                JLabel headline = new JLabel(message.getTypeName());
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, "newline 20, skip, span");
            }

            JComponent component = new JLabel();
            if (message.getDisplay() != null) {

                // TODO: Scale icons relative to font size.
                ImageIcon icon = getCanvas().getImageIcon(message.getDisplay(), false);
                if (icon != null && icon.getIconHeight() > 40) {
                    Image image = icon.getImage();
                    int newWidth = (int)((double)image.getWidth(null)/image.getHeight(null)*40.0);
                    image = image.getScaledInstance(newWidth, 40, Image.SCALE_SMOOTH);
                    icon.setImage(image);
                }

                if (message.getDisplay() instanceof Colony) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Colony) message.getDisplay()).getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (message.getDisplay() instanceof Unit) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Unit) message.getDisplay()).getLocation().getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (message.getDisplay() instanceof Player) {
                    component = new JLabel(icon);
                } else {
                    component = new JLabel(icon);
                }
            }
            reportPanel.add(component, "newline");

            final JTextPane textPane = getDefaultTextPane();
            insertMessage(textPane.getStyledDocument(), message, getCanvas().getClient().getMyPlayer());
            reportPanel.add(textPane);

            final JComponent label = component;
            if (message.getType() == ModelMessage.MessageType.WAREHOUSE_CAPACITY) {
                JButton ignoreButton = new JButton("x");
                ignoreButton.setToolTipText(Messages.message("model.message.ignore", message.getData()));
                ignoreButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        boolean flag = label.isEnabled();
                        getCanvas().getClient().getInGameController().ignoreMessage(message, flag);
                        textPane.setEnabled(!flag);
                        label.setEnabled(!flag);
                    }
                });
                reportPanel.add(ignoreButton);
            }
            final BooleanOption filterOption = options.getBooleanOption(message);
            // Message type can be filtered
            if (filterOption != null) {
                JButton filterButton = new JButton("X");
                filterButton.setToolTipText(Messages.message("model.message.filter", 
                    "%type%", message.getTypeName()));
                filterButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        boolean flag = filterOption.getValue();
                        filterOption.setValue(!flag);
                        textPane.setEnabled(!flag);
                        label.setEnabled(!flag);
                    }
                });
                reportPanel.add(filterButton);
            }
        }
    }

    private JComponent getHeadline(Object source) {
        JComponent headline;
        if (source == null) {
            return new JLabel();
        } else if (source instanceof Player) {
            Player player = (Player) source;
            headline = new JLabel(Messages.message("playerNation", 
                    "%player%", player.getName(),
                    "%nation%", player.getNationAsString()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(europe.getName());
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            JButton button = new JButton(Messages.message("model.message.marketPrices"));
            button.addActionListener(this);
            button.setActionCommand(getCanvas().getClient().getMyPlayer().getEurope().getId());
            headline = button;
        } else if (source instanceof Colony) {
            final Colony colony = (Colony) source;
            JButton button = new JButton(colony.getName());
            button.addActionListener(this);
            button.setActionCommand(colony.getId());
            headline = button;
        } else if (source instanceof Unit) {
            final Unit unit = (Unit) source;
            JButton button = new JButton(unit.getName());
            button.addActionListener(this);
            button.setActionCommand(unit.getLocation().getId());
            headline = button;
        } else if (source instanceof Nameable) {
            headline = new JLabel(((Nameable) source).getName());
        } else {
            headline = new JLabel(source.toString());
        }

        headline.setFont(smallHeaderFont);
        headline.setOpaque(false);
        headline.setForeground(LINK_COLOR);
        headline.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return headline;
    }

    private void insertMessage(StyledDocument document, ModelMessage message, Player player) {

        try {
            String input = Messages.message(message.getId());
            int start = input.indexOf('%');
            if (start == -1) {
                // no variables present
                insertText(document, input.substring(0));
                return;
            } else if (start > 0) {
                // output any string before the first occurrence of '%'
                insertText(document, input.substring(0, start));
            }
            int end;

            while ((end = input.indexOf('%', start + 1)) >= 0) {
                String var = input.substring(start, end + 1);
                String[] item = findReplacementData(message, var);
                if (item != null && var.equals(item[0])) {
                    // found variable to replace
                    if (var.equals("%colony%")) {
                        Colony colony = player.getColony(item[1]);
                        if (colony != null) {
                            insertLinkButton(document, colony, item[1]);
                        } else if (message.getSource() instanceof Tile) {
                            insertLinkButton(document, message.getSource(), item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if (var.equals("%europe%")) {
                        insertLinkButton(document, player.getEurope(), player.getEurope().getName());
                    } else if (var.equals("%unit%") || var.equals("%newName%")) {
                        Tile tile = null;
                        if (message.getSource() instanceof Unit) {
                            tile = ((Unit) message.getSource()).getTile();
                        } else if (message.getSource() instanceof Tile) {
                            tile = (Tile)message.getSource();
                        }
                        if (tile != null) {
                            insertLinkButton(document, tile, item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else {
                        insertText(document, item[1]);
                    }
                    start = end + 1;
                } else {
                    // found no variable to replace: either a single '%', or
                    // some unnecessary variable
                    insertText(document, input.substring(start, end));
                    start = end;
                }
            }

            // output any string after the last occurrence of '%'
            if (start < input.length()) {
                insertText(document, input.substring(start));
            }

        } catch(Exception e) {
            logger.warning(e.toString());
        }
    }
    
    private String[] findReplacementData(ModelMessage message, String variable) {
        String[] data = message.getData();
        if (data == null) {
            // message with no variables
            return null;
        } else if (data.length % 2 == 0) {
            for (int index = 0; index < data.length; index += 2) {
                if (variable.equals(data[index])) {
                    return new String[] { variable, data[index + 1] };
                }
            }
        } else {
            logger.warning("Data has a wrong format for message: " + message);
        }
        return null;
    }

    private void insertText(StyledDocument document, String text) throws Exception {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }


    private void insertLinkButton(StyledDocument document, FreeColGameObject object, String name)
        throws Exception {
        JButton button = getLinkButton(name, null, object.getId());
        button.addActionListener(this);
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ", document.getStyle("button"));
    }


    /**
     * This function analyzes an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(String.valueOf(OK))) {
            super.actionPerformed(event);
        } else {
            FreeColGameObject object = getCanvas().getClient().getGame().getFreeColGameObject(command);
            if (object instanceof Europe) {
                getCanvas().showEuropePanel();
            } else if (object instanceof Tile) {
                getCanvas().getGUI().setFocus(((Tile) object).getPosition());
            } else if (object instanceof Colony) {
                getCanvas().showColonyPanel((Colony) object);
            }
        }
    }

}
