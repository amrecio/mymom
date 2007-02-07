package net.sf.freecol.client;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.Queue;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.freecol.client.control.*;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.sound.*;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * The main control class for the FreeCol client. This class both starts and
 * keeps references to the GUI and the control objects.
 */
public final class FreeColClient {
    private static final Logger logger = Logger.getLogger(FreeColClient.class
            .getName());
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";
    // Control:
    private ConnectController connectController;
    private PreGameController preGameController;
    private PreGameInputHandler preGameInputHandler;
    private InGameController inGameController;
    private InGameInputHandler inGameInputHandler;
    private ClientModelController modelController;
    // Gui:
    private GraphicsDevice gd;
    private JFrame frame;
    private Canvas canvas;
    private GUI gui;
    private ImageLibrary imageLibrary;
    private MusicLibrary musicLibrary;
    private SfxLibrary sfxLibrary;
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
    private boolean singleplayer;
    private File clientOptionsFile;
    private ClientOptions clientOptions = new ClientOptions();
    private final ActionManager actionManager;
    public final Worker worker;
    /**
     * Indicated whether or not there is an open connection to the server. This
     * is not an indication of the existance of a Connection Object, but instead
     * it is an indication of an approved login to a server.
     */
    private boolean loggedIn = false;

    public FreeColClient(boolean windowed, Rectangle windowSize,
            ImageLibrary imageLibrary, MusicLibrary musicLibrary,
            SfxLibrary sfxLibrary, boolean startThreads) {
        this.windowed = windowed;
        this.imageLibrary = imageLibrary;
        this.musicLibrary = musicLibrary;
        this.sfxLibrary = sfxLibrary;
        actionManager = new ActionManager(this);
        // Control:
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        modelController = new ClientModelController(this);
        if (startThreads) {
            // Gui:
            final Rectangle theWindowSize = windowSize;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    startGUI(theWindowSize);
                }
            });
            worker = new Worker();
            worker.start();
            createFreeColDirs();
            loadClientOptions();
        } else
            worker = null;
    }

    /**
     * Creates a new <code>FreeColClient</code>. Creates the control objects
     * and starts the GUI.
     * 
     * @param windowed Determines if the <code>Canvas</code> should be
     *            displayed within a <code>JFrame</code> (when
     *            <code>true</code>) or in fullscreen mode (when
     *            <code>false</code>).
     * @param windowSize The size of the window if not in fullscreen mode.
     * @param imageLibrary The object holding the images.
     * @param musicLibrary The object holding the music.
     * @param sfxLibrary The object holding the sound effects.
     */
    public FreeColClient(boolean windowed, Rectangle windowSize,
            ImageLibrary imageLibrary, MusicLibrary musicLibrary,
            SfxLibrary sfxLibrary) {
        this(windowed, windowSize, imageLibrary, musicLibrary, sfxLibrary, true);
    }

    /**
     * Creates a freecol dir for the current operating system user. The
     * directory is created within the current user's home directory. This
     * directory will be called ".freecol" and underneath that directory there
     * will be a "save" directory created. All of this will only be done in case
     * they don't already exist.
     */
    private void createFreeColDirs() {
        String dir = System.getProperty("user.home");
        String fileSeparator = System.getProperty("file.separator");
        if (!dir.endsWith(fileSeparator)) {
            dir += fileSeparator;
        }
        dir += ".freecol";
        File file = new File(dir);
        if (file.exists() && file.isFile()) {
            logger
                    .warning("Could not create .freecol under ~ because there already exists a regular file with the same name.");
            return;
        } else if (!file.exists()) {
            file.mkdir();
        }
        clientOptionsFile = new File(dir, "options.xml");
        dir += fileSeparator + "save";
        file = new File(dir);
        if (file.exists() && file.isFile()) {
            logger
                    .warning("Could not create .freecol/save under ~ because there already exists a regular file with the same name.");
            return;
        } else if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    private void startGUI(Rectangle windowSize) {
        if (musicLibrary != null) {
            musicPlayer = new SoundPlayer(false, true, true);
        } else {
            musicPlayer = null;
        }
        // TODO: Start playing some music here.
        if (sfxLibrary != null) {
            sfxPlayer = new SoundPlayer(true, false, false);
        } else {
            sfxPlayer = null;
        }
        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        if (windowed) {
            frame = new WindowedFrame(windowSize);
        } else {
            if (!gd.isFullScreenSupported()) {
                logger
                        .info("It seems that full screen mode is not fully supported for this GraphicsDevice. Please try the \"--windowed\" option if you experience any graphical problems while running FreeCol.");
                /*
                 * We might want this behavior later: logger.warning("It seems
                 * that full screen mode is not supported for this
                 * GraphicsDevice! Using windowed mode instead."); windowed =
                 * true; setWindowed(true); frame = new
                 * WindowedFrame(windowSize);
                 */
            }
            frame = new FullScreenFrame(gd);
        }
        gui = new GUI(this, frame.getBounds(), imageLibrary);
        canvas = new Canvas(this, frame.getBounds(), gui);
        if (frame instanceof WindowedFrame) {
            ((WindowedFrame) frame).setCanvas(canvas);
        } else if (frame instanceof FullScreenFrame) {
            ((FullScreenFrame) frame).setCanvas(canvas);
        }
        frame.getContentPane().add(canvas);
        frame.setVisible(true);
        canvas.showMainPanel();
        gui.startCursorBlinking();
    }

    /**
     * Writes the client options to the default location.
     * 
     * @see ClientOptions
     */
    public void saveClientOptions() {
        saveClientOptions(clientOptionsFile);
    }

    /**
     * Writes the client options to the given file.
     * 
     * @param saveFile The file where the client options should be written.
     * @see ClientOptions
     */
    public void saveClientOptions(File saveFile) {
        Element element = getClientOptions().toXMLElement(
                Message.createNewDocument());
        // Write the XML Element to the file:
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xmlTransformer = factory.newTransformer();
            xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            try {
                PrintWriter out = new PrintWriter(
                        new FileOutputStream(saveFile));
                xmlTransformer.transform(new DOMSource(element),
                        new StreamResult(out));
                out.close();
            } catch (IOException ioe) {
                logger.warning("Could not store client options.");
            }
        } catch (TransformerException e) {
            logger.warning("TransformerException");
            return;
        }
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
        loadClientOptions(clientOptionsFile);
    }

    /**
     * Reads the {@link ClientOptions} from the given file.
     * 
     * @param loadFile The <code>File</code> to read the
     *            <code>ClientOptions</code> from.
     */
    public void loadClientOptions(File loadFile) {
        if (loadFile == null || !loadFile.exists()) {
            logger.warning("Could not find the client options file.");
            return;
        }
        try {
            Message message = new Message(new FileInputStream(loadFile));
            Element element = message.getDocument().getDocumentElement();
            getClientOptions().readFromXMLElement(element);
        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            StringWriter sw = new StringWriter();
            x.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        } catch (NullPointerException e) {
            logger.warning("The given file does not contain client options.");
        } catch (IOException e) {
            logger.warning("Exception while loading client options.");
        }
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
     * Gets the <code>GUI</code> that is beeing used to draw the map on the
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
    private void setWindowed(boolean windowed) {
        this.windowed = windowed;
    }

    /**
     * Sets wether or not this game is a singleplayer game.
     * 
     * @param singleplayer Indicates wether or not this game is a singleplayer
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
     * Plays the given sound effect.
     * 
     * @param sound The key sound effect given by {@link SfxLibrary}.
     */
    public void playSound(int sound) {
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
                Element query = Message
                        .createNewRootElement("getRandomNumbers");
                query.setAttribute("n", String.valueOf(VALUES_PER_CALL));
                // We expect client != null when logged in
                Element answer = getClient().ask(query);
                if (answer != null
                        && "getRandomNumbersConfirmed".equals(answer
                                .getTagName())) {
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
