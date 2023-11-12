package pleasehireus;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.oauth2.Oauth2Scopes;

import pleasehireus.handlers.OauthCallbackHandler;
import pleasehireus.handlers.OauthHandler;

/**
 * Hello world!
 *
 */
public class App {
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    public static final String TOKENS_DIRECTORY_PATH = "tokens";
    public static final List<String> SCOPES = Arrays.asList(Oauth2Scopes.USERINFO_EMAIL, GmailScopes.GMAIL_READONLY);



    public static void main(String[] args) throws Exception {
        GoogleClientSecrets clientSecrets;
        try (BufferedReader r = Files.newBufferedReader(Paths.get("../client_secret_441026547871-muj075stfof2j8eord3j1fdibagspu5b.apps.googleusercontent.com.json"))) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, r);
        }
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, App.JSON_FACTORY, clientSecrets, App.SCOPES)
                // .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(App.TOKENS_DIRECTORY_PATH)))
                .setCredentialDataStore(new FileDataStoreFactory(new java.io.File(App.TOKENS_DIRECTORY_PATH)).getDataStore("creds_lol"))
                .build();

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
        contextCollection.addHandler(new ContextHandler(new OauthHandler(clientSecrets, flow), "/login"));
        contextCollection.addHandler(new OauthCallbackHandler(clientSecrets, flow));
        // contextCollection.addHandler(new ServletC);

        server.setHandler(contextCollection);
        server.setDefaultHandler(new OauthCallbackHandler(clientSecrets, flow));

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
