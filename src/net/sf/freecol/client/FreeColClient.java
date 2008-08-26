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

package net.sf.freecol.client;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ClientModelController;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameInputHandler;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.PreGameInputHandler;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FullScreenFrame;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.WindowedFrame;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.client.gui.sound.SoundLibrary;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;

/**
 * The main control class for the FreeCol client. This class both starts and
 * keeps references to the GUI and the control objects.
 */
public final class FreeColClient {

    private static final Logger logger = Logger.getLogger(FreeColClient.class.getName());

    // Control:
    private ConnectController connectController;

    private PreGameController preGameController;

    private PreGameInputHandler preGameInputHandler;

    private InGameController inGameController;

    private InGameInputHandler inGameInputHandler;

    private ClientModelController modelController;
    
    private MapEditorController mapEditorController;
    

    // Gui:
    private GraphicsDevice gd;

    private JFrame frame;

    private Canvas canvas;

    private GUI gui;

    private ImageLibrary imageLibrary;

    private MusicLibrary musicLibrary;

    private SfxLibrary sfxLibrary;

    @SuppressWarnings("unused")
    private SoundPlayer musicPlayer;

    private SoundPlayer sfxPlayer;

    // Networking:
    /**
     * The network <code>Client</code> that can be used to send messages to
     * the server.
     */
    private Client client;

    // Model:
    private Game game;

    private final PseudoRandom _random = new ClientPseudoRandom();

    /** The player "owning" this client. */
    private Player player;

    /** The Server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    private boolean windowed;
    
    private boolean mapEditor;

    private boolean singleplayer;

    private final ActionManager actionManager;
    
    private ClientOptions clientOptions;

    public final Worker worker;

    /**
     * Indicated whether or not there is an open connection to the server. This
     * is not an indication of the existence of a Connection Object, but instead
     * it is an indication of an approved login to a server.
     */
    private boolean loggedIn = false;
    
    private Rectangle windowBounds;


    /**
     * Creates a new <code>FreeColClient</code>. Creates the control objects
     * and starts the GUI.
     * 
     * @param windowed Determines if the <code>Canvas</code> should be
     *            displayed within a <code>JFrame</code> (when
     *            <code>true</code>) or in fullscreen mode (when
     *            <code>false</code>).
     * @param innerWindowSize The inner size of the window (borders not included).
     * @param imageLibrary The object holding the images.
     * @param musicLibrary The object holding the music.
     * @param sfxLibrary The object holding the sound effects.
     */
    public FreeColClient(boolean windowed, final Dimension innerWindowSize, 
                         ImageLibrary imageLibrary, MusicLibrary musicLibrary,
                         SfxLibrary sfxLibrary) {
        boolean headless = "true".equals(System.getProperty("java.awt.headless", "false"));
        this.windowed = windowed;
        this.imageLibrary = imageLibrary;
        this.musicLibrary = musicLibrary;
        this.sfxLibrary = sfxLibrary;
        
        mapEditor = false;
        
        clientOptions = new ClientOptions();
        if (FreeCol.getClientOptionsFile() != null
                && FreeCol.getClientOptionsFile().exists()) {
            clientOptions.load(FreeCol.getClientOptionsFile());
        }
        actionManager = new ActionManager(this);
        if (!headless) {
            actionManager.initializeActions();
        }
        // Control:
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        modelController = new ClientModelController(this);
        mapEditorController = new MapEditorController(this);
        
        // Gui:
        if (!headless) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        startGUI(innerWindowSize);
                    }
                });
        }
        worker = new Worker();
        worker.start();
        
        if (FreeCol.getClientOptionsFile() != null
                && FreeCol.getClientOptionsFile().exists()) {
            if (!headless) {
                Option o = clientOptions.getObject(ClientOptions.LANGUAGE);
                o.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            if (((Language) e.getNewValue()).getKey().equals(LanguageOption.AUTO)) {
                                canvas.showInformationMessage("autodetectLanguageSelected");
                            } else {
                                Locale l = ((Language) e.getNewValue()).getLocale();
                                Messages.setMessageBundle(l);
                                canvas.showInformationMessage("newLanguageSelected", "%language%", l.getDisplayName());
                            }
                        }
                    });
            }
        }
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    private void startGUI(Dimension innerWindowSize) {
        final AudioMixerOption amo = (AudioMixerOption) getClientOptions().getObject(ClientOptions.AUDIO_MIXER);
        if (musicLibrary != null) {
            musicPlayer = new SoundPlayer(amo,
                    (PercentageOption) getClientOptions().getObject(ClientOptions.MUSIC_VOLUME),
                    false,
                    true);
            playMusic("intro");
        } else {
            musicPlayer = null;
        }
        if (sfxLibrary != null) {
            sfxPlayer = new SoundPlayer(amo,
                    (PercentageOption) getClientOptions().getObject(ClientOptions.SFX_VOLUME),
                    true,
                    false);
        } else {
            sfxPlayer = null;
        }
        
        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!windowed) {
            if (!gd.isFullScreenSupported()) {
                String fullscreenNotSupported = "\nIt seems that full screen mode is not fully supported for this\nGraphicsDevice. Please try the \"--windowed\" option if you\nexperience any graphical problems while running FreeCol.";
                logger.info(fullscreenNotSupported);
                System.out.println(fullscreenNotSupported);
                /*
                 * We might want this behavior later: logger.warning("It seems
                 * that full screen mode is not supported for this
                 * GraphicsDevice! Using windowed mode instead."); windowed =
                 * true; setWindowed(true); frame = new
                 * WindowedFrame(windowSize);
                 */
            }
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            innerWindowSize = new Dimension(bounds.width - bounds.x, bounds.height - bounds.y);
        }
        gui = new GUI(this, innerWindowSize, imageLibrary);
        canvas = new Canvas(this, innerWindowSize, gui);
        changeWindowedMode(windowed);

        UnitType galleonType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
        frame.setIconImage(imageLibrary.getUnitImageIcon(galleonType).getImage());
        
        SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		canvas.showMainPanel();
        	}
        });
        gui.startCursorBlinking();
    }
    
    /**
     * Change the windowed mode.
     * @param windowed Use <code>true</code> for windowed mode
     *      and <code>false</code> for fullscreen mode.
     */
    public void changeWindowedMode(boolean windowed) {
        if (frame != null) {
            if (frame instanceof WindowedFrame) {
                windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        this.windowed = windowed;
        if (windowed) {
            frame = new WindowedFrame();
        } else {
            frame = new FullScreenFrame(gd);
        }
        if (frame instanceof WindowedFrame) {
            ((WindowedFrame) frame).setCanvas(canvas);
            frame.getContentPane().add(canvas);
            if (windowBounds != null) {
                frame.setBounds(windowBounds);
            } else {
                frame.pack();
            }
        } else if (frame instanceof FullScreenFrame) {
            ((FullScreenFrame) frame).setCanvas(canvas);
            frame.getContentPane().add(canvas);
            canvas.setSize(frame.getSize());
        }
        gui.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }
    
    /**
     * Checks if the application is displayed in a window.
     * @return <code>true</code> if the application is currently
     *      displayed in a frame, and <code>false</code> if
     *      currently in fullscreen mode.
     * @see #changeWindowedMode
     */
    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Writes the client options to the default location.
     * 
     * @see ClientOptions
     */
    public void saveClientOptions() {
        saveClientOptions(FreeCol.getClientOptionsFile());
    }

    public void setMapEditor(boolean mapEditor) {
        this.mapEditor = mapEditor;
    }
    
    public boolean isMapEditor() {
        return mapEditor;
    }
    
    /**
     * Writes the client options to the given file.
     * 
     * @param saveFile The file where the client options should be written.
     * @see ClientOptions
     */
    public void saveClientOptions(File saveFile) {
        getClientOptions().save(saveFile);
    }

    /**
     * Gets the <code>ImageLibrary</code>.
     * 
     * @return The <code>ImageLibrary</code>.
     */
    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    /**
     * Reads the {@link ClientOptions} from the given file.
     */
    public void loadClientOptions() {
        loadClientOptions(FreeCol.getClientOptionsFile());
    }

    /**
     * Reads the {@link ClientOptions} from the given file.
     * 
     * @param loadFile The <code>File</code> to read the
     *            <code>ClientOptions</code> from.
     */
    public void loadClientOptions(File loadFile) {
        getClientOptions().load(loadFile);
    }

    /**
     * Gets the object responsible for keeping and updating the actions.
     * 
     * @return The <code>ActionManager</code>.
     */
    public ActionManager getActionManager() {
        return actionManager;
    }

    /**
     * Returns the object keeping the current client options.
     * 
     * @return The <code>ClientOptions</code>.
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
    }
    
    public MapEditorController getMapEditorController() {
        return mapEditorController;
    }

    /**
     * Gets the <code>Player</code> that uses this client.
     * 
     * @return The <code>Player</code> made to represent this clients user.
     * @see #setMyPlayer(Player)
     */
    public Player getMyPlayer() {
        return player;
    }

    /**
     * Sets the <code>Player</code> that uses this client.
     * 
     * @param player The <code>Player</code> made to represent this clients
     *            user.
     * @see #getMyPlayer()
     */
    public void setMyPlayer(Player player) {
        this.player = player;
    }

    /**
     * Sets the <code>FreeColServer</code> which has been started by the
     * client gui.
     * 
     * @param freeColServer The <code>FreeColServer</code>.
     * @see #getFreeColServer()
     */
    public void setFreeColServer(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Gets the <code>FreeColServer</code> started by the client.
     * 
     * @return The <code>FreeColServer</code> or <code>null</code> if no
     *         server has been started.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Sets the <code>Game</code> that we are currently playing.
     * 
     * @param game The <code>Game</code>.
     * @see #getGame
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Gets the <code>Game</code> that we are currently playing.
     * 
     * @return The <code>Game</code>.
     * @see #setGame
     */
    public Game getGame() {
        return game;
    }

    /**
     * Gets the <code>Canvas</code> this client uses to display the
     * GUI-components.
     * 
     * @return The <code>Canvas</code>.
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Gets the <code>GUI</code> that is being used to draw the map on the
     * {@link Canvas}.
     * 
     * @return The <code>GUI</code>.
     */
    public GUI getGUI() {
        return gui;
    }

    /**
     * Quits the application without any questions.
     */
    public void quit() {
        getConnectController().quitGame(true);
        if (!windowed) {
            gd.setFullScreenWindow(null);
        }
        System.exit(0);
    }

    /**
     * Continue playing after win the game
     */
    public void continuePlaying() {
        Element continueElement = Message.createNewRootElement("continuePlaying");
        client.send(continueElement);
    }

    /**
     * Checks if this client is the game admin.
     * 
     * @return <i>true</i> if the client is the game admin and <i>false</i>
     *         otherwise. <i>false</i> is also returned if a game have not yet
     *         been started.
     */
    public boolean isAdmin() {
        if (getMyPlayer() == null) {
            return false;
        }
        return getMyPlayer().isAdmin();
    }

    /**
     * Sets the type of main window to display.
     * 
     * @param windowed The main window is a full-screen window if set to
     *            <i>false</i> and a normal window otherwise.
     */
    // private void setWindowed(boolean windowed) {
    // this.windowed = windowed;
    // }
    /**
     * Sets whether or not this game is a singleplayer game.
     * 
     * @param singleplayer Indicates whether or not this game is a singleplayer
     *            game.
     * @see #isSingleplayer
     */
    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }

    /**
     * Is the user playing in singleplayer mode.
     * 
     * @return <i>true</i> if the user is playing in singleplayer mode and
     *         <i>false</i> otherwise.
     * @see #setSingleplayer
     */
    public boolean isSingleplayer() {
        return singleplayer;
    }

    /**
     * Gets the controller responsible for starting a server and connecting to
     * it.
     * 
     * @return The <code>ConnectController</code>.
     */
    public ConnectController getConnectController() {
        return connectController;
    }

    /**
     * Gets the controller that will be used before the game has been started.
     * 
     * @return The <code>PreGameController</code>.
     */
    public PreGameController getPreGameController() {
        return preGameController;
    }

    /**
     * Gets the input handler that will be used before the game has been
     * started.
     * 
     * @return The <code>PreGameInputHandler</code>.
     */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }

    /**
     * Gets the controller that will be used when the game has been started.
     * 
     * @return The <code>InGameController</code>.
     */
    public InGameController getInGameController() {
        return inGameController;
    }

    /**
     * Gets the input handler that will be used when the game has been started.
     * 
     * @return The <code>InGameInputHandler</code>.
     */
    public InGameInputHandler getInGameInputHandler() {
        return inGameInputHandler;
    }

    /**
     * Gets the <code>ClientModelController</code>.
     * 
     * @return The <code>ClientModelController</code>.
     */
    public ClientModelController getModelController() {
        return modelController;
    }

    /**
     * Sets the <code>Client</code> that shall be used to send messages to the
     * server.
     * 
     * @param client the <code>Client</code>
     * @see #getClient
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Gets the <code>Client</code> that can be used to send messages to the
     * server.
     * 
     * @return the <code>Client</code>
     * @see #setClient
     */
    public Client getClient() {
        return client;
    }

    /**
     * Plays the music.
     */
    public void playMusic(String music) {
        if (musicPlayer != null) {
            musicPlayer.play(musicLibrary.get(music));
        }
    }
    
    /**
     * Plays a random music from the given playlist.
     */
    public void playMusicOnce(String music) {
        if (musicPlayer != null) {
            musicPlayer.playOnce(musicLibrary.get(music));
        }
    }
    
    /**
     * Plays a random music from the given playlist.
     * @param delay A delay before playing the sound (ms).
     */
    public void playMusicOnce(String music, int delay) {
        if (musicPlayer != null) {
            musicPlayer.playOnce(musicLibrary.get(music), delay);
        }
    }
    
    /**
     * Plays the given sound effect.
     * 
     * @param sound The key sound effect given by {@link SfxLibrary}.
     */
    public void playSound(String sound) {
        if (sfxPlayer != null) {
            sfxPlayer.play(sfxLibrary.get(sound));
        }
    }

    /**
     * Plays the given sound effect.
     * 
     * @param sound The key sound effect given by {@link SfxLibrary}.
     */
    public void playSound(SoundLibrary.SoundEffect sound) {
        if (sfxPlayer != null) {
            sfxPlayer.play(sfxLibrary.get(sound));
        }
    }

    /**
     * Returns <i>true</i> if this client is logged in to a server or <i>false</i>
     * otherwise.
     * 
     * @return <i>true</i> if this client is logged in to a server or <i>false</i>
     *         otherwise.
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Sets whether or not this client is logged in to a server.
     * 
     * @param loggedIn An indication of whether or not this client is logged in
     *            to a server.
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Get the pseudo-random number generator for the client.
     * 
     * @return random number generator.
     */
    public PseudoRandom getPseudoRandom() {
        return _random;
    }


    /**
     * This class provides server-generated random numbers for client-side use.
     * It requires a server connection. If the connection is unavailable it will
     * generate random numbers on the client side rather than failing.
     */
    private class ClientPseudoRandom implements PseudoRandom {
        ClientPseudoRandom() {
            values = new LinkedList<Integer>();
            offlineRandom = new Random();
        }

        /**
         * Get the next pseudo-random integer between 0 and n.
         * 
         * @param n The upper bound (exclusive).
         * @return random number between 0 (inclusive) and n (exclusive).
         */
        public int nextInt(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("n must be positive!");
            }
            // TODO (Erik): get random int in given range.
            // This may not be good enough, as the low bits may be less
            // random than the entire range. See the Random class for a
            // more advanced implementation.
            return Math.abs(nextInt() % n);
        }

        /**
         * Get the next pseudo-random integer.
         * <p>
         * The method requires one network call per {@link #VALUES_PER_CALL}
         * requests. If multiple threads are active a single thread may
         * theoretically have to wait indefinitely, but in practice this is not
         * very likely.
         * 
         * @return next random integer.
         */
        private int nextInt() {
            Integer i = pop();
            while (i == null) {
                getNewNumbers();
                i = pop();
            }
            return i.intValue();
        }

        /**
         * Get new numbers to the queue from the server, generate on client side
         * if not connected. The method is guaranteed to generate at least one
         * new number.
         */
        private void getNewNumbers() {
            int valuesAdded = 0;
            if (isLoggedIn()) {
                Element query = Message.createNewRootElement("getRandomNumbers");
                query.setAttribute("n", String.valueOf(VALUES_PER_CALL));
                // We expect client != null when logged in
                Element answer = getClient().ask(query);
                if (answer != null && "getRandomNumbersConfirmed".equals(answer.getTagName())) {
                    for (String s : answer.getAttribute("result").split(",")) {
                        push(new Integer(s));
                        ++valuesAdded;
                    }
                } else {
                    logger.warning("Expected getRandomNumbersConfirmed, got "
                            + (answer != null ? answer.getTagName() : "null"));
                }
            }
            // Fallback solution on errors (we don't want to crash the game)
            if (valuesAdded < 1) {
                logger.fine("Generating random number on client side");
                push(Integer.valueOf(offlineRandom.nextInt()));
            }
        }

        private synchronized void push(Integer i) {
            values.offer(i);
        }

        private synchronized Integer pop() {
            return values.poll();
        }


        private final Random offlineRandom;

        private final Queue<Integer> values;

        private static final int VALUES_PER_CALL = 100;
    }
}

