package lucasjc;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;

/**
 * Main class
 */
public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	
	/**
	 * Start application
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		final ServerWrapper server = new ServerWrapper(8081);
		
		// create a new namespace
		SocketIoNamespace namespace = server.getSocketIoServer().namespace("/");
		
		// handle connection event
		namespace.on("connection", params -> {
			
			final SocketIoSocket socket = (SocketIoSocket) params[0];
			String id = socket.getId();
			LOG.info("New connection {} at {}", id, socket.getNamespace().getName());
			LOG.info("Received data: {}", socket.getConnectData());
			
			// send a message to lobby room indicating a new connection
			namespace.broadcast("lobby", "new-connection", id);
			
			// add new socket to lobby room
			socket.joinRoom("lobby");
			
			// send a message to new socket
			socket.send("message", "Welcome " + id);
		});

		// set up a periodic message to lobby room
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				namespace.broadcast("lobby", "message", "This is a periodic message");
			}
		}, 1000, 5000);
		
		server.start();
	}
	
}
