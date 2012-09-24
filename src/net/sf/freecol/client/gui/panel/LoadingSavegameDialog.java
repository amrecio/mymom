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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Dialog for setting some options when loading a game.
 */
public final class LoadingSavegameDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(LoadingSavegameDialog.class.getName());


    private JPanel buttons = new JPanel(new FlowLayout());

    private JLabel header;

    private JRadioButton singlePlayer;

    private JRadioButton privateMultiplayer;

    private JRadioButton publicMultiplayer;

    private JTextField serverNameField;

    private JSpinner portField;


    /**
     * Creates a dialog to set the options for loading a saved game.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     */
    public LoadingSavegameDialog(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui);
        setLayout(new BorderLayout());

        buttons.add(okButton);
        buttons.add(cancelButton);

        // Header:
        header = new JLabel(Messages.message("LoadingSavegame.title"), JLabel.CENTER);
        header.setFont(mediumHeaderFont);
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        // Panel:
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel(Messages.message("LoadingSavegame.serverName"), JLabel.LEFT));
        panel.add(p1);
        serverNameField = new JTextField();
        panel.add(serverNameField);

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2.add(new JLabel(Messages.message("LoadingSavegame.port"), JLabel.LEFT));
        panel.add(p2);
        portField = new JSpinner(new SpinnerNumberModel(FreeCol.getDefaultPort(), 1, 65536, 1));
        panel.add(portField);

        ButtonGroup bg = new ButtonGroup();
        singlePlayer = new JRadioButton(Messages.message("LoadingSavegame.singlePlayer"));
        bg.add(singlePlayer);
        panel.add(singlePlayer);
        privateMultiplayer = new JRadioButton(Messages.message("LoadingSavegame.privateMultiplayer"));
        bg.add(privateMultiplayer);
        panel.add(privateMultiplayer);
        publicMultiplayer = new JRadioButton(Messages.message("LoadingSavegame.publicMultiplayer"));
        bg.add(publicMultiplayer);
        panel.add(publicMultiplayer);

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel, BorderLayout.CENTER);

        // Buttons:
        add(buttons, BorderLayout.SOUTH);

        setSize(getPreferredSize());

    }


    public boolean isSinglePlayer() {
        return singlePlayer.isSelected();
    }

    public boolean isPublic() {
        return publicMultiplayer.isSelected();
    }

    public int getPort() {
        return ((Integer) portField.getValue()).intValue();
    }

    @Override
    public String getName() {
        return serverNameField.getName();
    }

    public void initialize(boolean publicServer, boolean singlePlayer) {

        this.singlePlayer.setSelected(false);
        this.privateMultiplayer.setSelected(false);
        this.publicMultiplayer.setSelected(false);

        if (singlePlayer) {
            this.singlePlayer.setSelected(true);
        } else if (publicServer) {
            this.publicMultiplayer.setSelected(true);
        } else {
            this.privateMultiplayer.setSelected(true);
        }

        this.serverNameField.setText("");
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getGUI().removeFromCanvas(this);
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            getGUI().removeFromCanvas(this);
            setResponse(Boolean.FALSE);
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }
}
