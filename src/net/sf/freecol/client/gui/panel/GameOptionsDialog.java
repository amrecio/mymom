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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.common.model.GameOptions;

import net.miginfocom.swing.MigLayout;


/**
 * Dialog for changing the {@link net.sf.freecol.common.model.GameOptions}.
 */
public final class GameOptionsDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(GameOptionsDialog.class.getName());

    private JButton load, save, reset;

    private OptionGroupUI ui;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public GameOptionsDialog(Canvas parent, boolean editable) {
        super(parent);
        setLayout(new MigLayout("wrap 1, fill"));

        load = new JButton(Messages.message("load"));
        load.setActionCommand(LOAD);
        load.addActionListener(this);

        save = new JButton(Messages.message("save"));
        save.setActionCommand(SAVE);
        save.addActionListener(this);

        reset = new JButton(Messages.message("reset"));
        reset.setActionCommand(RESET);
        reset.addActionListener(this);

        FreeColPanel.enterPressesWhenFocused(okButton);
        setCancelComponent(cancelButton);

        // Header:
        add(getDefaultHeader(Messages.message("gameOptions")), "center");

        // Options:
        ui = new OptionGroupUI(getSpecification().getOptionGroup("gameOptions"), editable);
        add(ui, "newline 20, grow");

        // Buttons:
        if (editable) {
            add(okButton, "newline 20, split 5, tag ok");
            add(cancelButton, "tag cancel");
            add(load);
            add(save);
            add(reset);
        } else {
            add(okButton, "newline 20, tag ok");
        }

        // Set special cases

        // Disable victory option "All humans defeated"
        //when playing single player
        if (editable && getClient().isSingleplayer()){
            BooleanOptionUI comp = (BooleanOptionUI) ui.getOptionUI(GameOptions.VICTORY_DEFEAT_HUMANS);

            comp.setValue(false);
            comp.setEnabled(false);
        }
        setSize(640, 480);

    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(640, 480);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.unregister();
            ui.updateOption();
            getClient().getPreGameController().sendGameOptions();
            getCanvas().remove(this);
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(Boolean.FALSE);
        } else if (SAVE.equals(command)) {
            FileFilter[] filters = new FileFilter[] { FreeColDialog.getFGOFileFilter(),
                                                      FreeColDialog.getFSGFileFilter(),
                                                      FreeColDialog.getGameOptionsFileFilter() };
            File saveFile = getCanvas().showSaveDialog(FreeCol.getSaveDirectory(), ".fgo", filters, "");
            if (saveFile != null) {
                ui.updateOption();
                getGame().getSpecification().getOptionGroup("gameOptions").save(saveFile);
            }
        } else if (LOAD.equals(command)) {
            File loadFile = getCanvas().showLoadDialog(FreeCol.getSaveDirectory(),
                                                       new FileFilter[] {
                                                           FreeColDialog.getFGOFileFilter(),
                                                           FreeColDialog.getFSGFileFilter(),
                                                           FreeColDialog.getGameOptionsFileFilter()
                                                       });
            if (loadFile != null) {
                try {
                    FileInputStream in = new FileInputStream(loadFile);
                    getGame().getSpecification().loadFragment(in);
                    in.close();
                } catch(Exception e) {
                    logger.warning("Failed to load game options from " + loadFile.getName());
                }
            }
        } else if (RESET.equals(command)) {
            ui.reset();
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }
}
