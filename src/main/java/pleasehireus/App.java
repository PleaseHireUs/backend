package pleasehireus;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");

        // Create a Server instance.
        Server server = new Server(threadPool);

        // Create a ServerConnector to accept connections from clients.
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        // Add the Connector to the Server
        server.addConnector(connector);

        // Set a simple Handler to handle requests/responses.

        ContextHandlerCollection contextCollection = new ContextHandlerCollection();
        contextCollection.addHandler(new ContextHandler(new TeapotHandler(), "/teapot"));

        server.setHandler(contextCollection);

        // Start the Server to start accepting connections from clients.
        
        server.start();
        System.out.println("http://localhost:" + connector.getPort());
    }

    static class TeapotHandler extends Handler.Abstract {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            // Implement the shop, remembering to complete the callback.
            response.setStatus(418);
            callback.succeeded();
            return true;
        }
    }
}
