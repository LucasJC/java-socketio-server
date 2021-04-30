package lucasjc;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoServerOptions;
import io.socket.engineio.server.JettyWebSocketHandler;
import io.socket.socketio.server.SocketIoServer;

/**
 * Wrapper for a Jetty embedded server integrated with socket.io
 */
final class ServerWrapper {

    private final int port;
    private final Server server;
    private final EngineIoServer mEngineIoServer;
    private final SocketIoServer mSocketIoServer;

    /**
     * @param port
     */
    ServerWrapper(int port) {
    	this.port = port;
        this.server = new Server(port);
        
        final EngineIoServerOptions opts = EngineIoServerOptions.newFromDefault();
        opts.setPingInterval(20000);
        opts.setPingTimeout(30000);
        mEngineIoServer = new EngineIoServer(opts);
        mSocketIoServer = new SocketIoServer(mEngineIoServer);

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
            /**
			 */
			private static final long serialVersionUID = 1L;
		
			@Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                mEngineIoServer.handleRequest(new HttpServletRequestWrapper(request) {
                    @Override
                    public boolean isAsyncSupported() {
                        return false;
                    }
                }, response);
            }
        }), "/socket.io/*");

        try {
            WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(servletContextHandler);
            webSocketUpgradeFilter.addMapping(
                    new ServletPathSpec("/socket.io/*"),
                    (servletUpgradeRequest, servletUpgradeResponse) -> new JettyWebSocketHandler(mEngineIoServer));
        } catch (ServletException ex) {
            ex.printStackTrace();
        }

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] { servletContextHandler });
        server.setHandler(handlerList);
    }

    /**
     * start server
     * @throws Exception
     */
    void start() throws Exception {
        server.start();
    }

    /**
     * stop server
     * @throws Exception
     */
    void stop() throws Exception {
        server.stop();
    }

    /**
     * @return server port
     */
    int getPort() {
        return port;
    }

    /**
     * @return socket.io server instance
     */
    SocketIoServer getSocketIoServer() {
        return mSocketIoServer;
    }
}