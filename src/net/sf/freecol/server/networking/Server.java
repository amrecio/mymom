
package net.sf.freecol.server.networking;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.model.FreeColGameObject;

import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.Document;



/**
* The networking server in which new clients can connect and methods
* like <code>sendToAll</code> are kept.
*
* <br><br>
*
* When a new client connects to the server a new {@link Connection}
* is made, with {@link UserConnectionHandler} as the control object.
*
* @see net.sf.freecol.common.networking
*/
public final class Server extends Thread {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(Server.class.getName());


    /** The public "well-known" socket to which clients may connect. */
    private ServerSocket serverSocket;

    /** A hash of Connection objects, keyed by the Socket they relate to. */
    private HashMap connections = new HashMap();

    /** Whether to keep running the main loop that is awaiting new client connections. */
    private boolean running = true;

    /** The owner of this <code>Server</code>. */
    private FreeColServer freeColServer;

    /** The TCP port that is beeing used for the public socket. */
    private int port;





    /**
     * Creates a new network server. Use {@link #run server.start()} to start
     * listening for new connections.
     *
     * @param freeColServer The owner of this <code>Server</code>.
     * @param port The TCP port to use for the public socket.
     * @throws IOException if the public socket cannot be created.
     */
    public Server(FreeColServer freeColServer, int port) throws IOException {
        this.freeColServer = freeColServer;
        this.port = port;
        //serverSocket = new ServerSocket(port, freeColServer.getMaximumPlayers());
        serverSocket = new ServerSocket(port);
    }




    /**
    * Starts the thread's processing. Contains the loop that is waiting for new
    * connections to the public socket. When a new client connects to the server
    * a new {@link Connection} is made, with {@link UserConnectionHandler} as 
    * the control object.
    */
    public void run() {
        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                logger.info("Got client connection from " + clientSocket.getInetAddress().toString());
                Connection connection = new Connection(clientSocket, freeColServer.getUserConnectionHandler());
                connections.put(clientSocket, connection);
            } catch (IOException e) {
                logger.warning("Accept/connect exception " + e.toString());
            }
        }
    }


    /**
    * Sends a network message to all connections except <code>exceptConnection</code>
    * (if the argument is non-null).
    *
    * @param element The root element of the message to send.
    * @param exceptConnection If non-null, the <code>Connection</code> not to send to.
    */
    public void sendToAll(Element element, Connection exceptConnection) {
        Iterator connectionIterator = connections.values().iterator();

        while (connectionIterator.hasNext()) {
            Connection connection = (Connection) connectionIterator.next();
            if (connection != exceptConnection) {
                try {
                    connection.send(element);
                } catch (IOException e) {
                    logger.warning("Exception while attempting to send to " + connection);
                }
            }
        }
    }
    
    
    /**
    * Sends a network message to all connections.
    * @param element The root element of the message to send.
    */
    public void sendToAll(Element element) {
        sendToAll(element, null);
    }

    
    /**
    * Gets the TCP port that is beeing used for the public socket.
    * @return The TCP port.
    */
    public int getPort() {
        return port;
    }


    /**
    * Sets the specified <code>MessageHandler</code> to all connections.
    * @param messageHandler The <code>MessageHandler</code>.
    */
    public void setMessageHandlerToAllConnections(MessageHandler messageHandler) {
        Iterator connectionIterator = connections.values().iterator();

        while (connectionIterator.hasNext()) {
            Connection connection = (Connection)connectionIterator.next();
            connection.setMessageHandler(messageHandler);
        }
    }


    /**
    * Gets an iterator of every connection to this server.
    *
    * @return The <code>Iterator</code>.
    * @see Connection
    */
    public Iterator getConnectionIterator() {
        return connections.values().iterator();
    }


    /**
    * Shuts down the server thread.
    */
    public void shutdown() {
        running = false;
        
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warning("Could not close the server socket!");
        }
    }
    
    
    /**
    * Gets a <code>Connection</code> identified by a <code>Socket</code>.
    *
    * @param socket The <code>Socket</code> that identifies the
    *               <code>Connection</code>
    * @return The <code>Connection</code>.
    */
    public Connection getConnection(Socket socket) {
        return (Connection) connections.get(socket);
    }
    
    /**
    * Adds a (usually Dummy)Connection into the hashmap.
    * @param connection The connection to add.
    * @param fakesocket The false socket number to use.
    */
    public void addConnection(Connection connection, int fakesocket) {
        connections.put(new Socket(), connection);
    }
}
