package pleasehireus;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.BsonDocument;
import org.bson.Document;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import pleasehireus.handlers.AddJobHandler;
import pleasehireus.handlers.OauthCallbackHandler;
import pleasehireus.handlers.OauthHandler;
import pleasehireus.handlers.StatusHandler;

/**
 * Hello world!
 *
 */
public class App {
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    public static final String TOKENS_DIRECTORY_PATH = "tokens";
    public static final List<String> SCOPES = Arrays.asList(Oauth2Scopes.USERINFO_EMAIL, GmailScopes.GMAIL_READONLY);
    public static record GmailAccountCred(Credential credential, String email) {
    }
    public static final ConcurrentHashMap<String, GmailAccountCred> TOKEN_2_GOOGLE_MAP = new ConcurrentHashMap<>();


    public static void main(String[] args) throws Exception {
        mongoDbCrap();
        GoogleClientSecrets clientSecrets;
        try (BufferedReader r = Files.newBufferedReader(Paths.get("./client_secret_441026547871-muj075stfof2j8eord3j1fdibagspu5b.apps.googleusercontent.com.json"))) {
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
        // contextCollection.addHandler(new OauthCallbackHandler(clientSecrets, flow));
        contextCollection.addHandler(new ContextHandler(new AddJobHandler(), "/addjob"));
        contextCollection.addHandler(new ContextHandler(new StatusHandler(), "/status"));
        // contextCollection.addHandler(new ServletC);

        server.setHandler(contextCollection);
        server.setDefaultHandler(new OauthCallbackHandler(clientSecrets, flow));

        // Start the Server to start accepting connections from clients.

        server.start();
        System.out.println("http://localhost:" + connector.getPort());
    }

    public static void mongoDbCrap() {
        // Set system properties via commandline or programmatically
        // System.setProperty("javax.net.ssl.keyStore", "<path_to_keystore>");
        // System.setProperty("javax.net.ssl.keyStorePassword", "<keystore_password>");

        String uri = "mongodb+srv://user-auth:MadHack123@cluster0.5jai7nf.mongodb.net/?retryWrites=true&w=majority";
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("JonApplicationData");
        MongoCollection<Document> collection = database.getCollection("UserData");
        Database.collection = collection;
        Database.load();
    }

    public static String randomID() {
        SecureRandom sr = new SecureRandom();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            token.append((char)('0' + sr.nextInt(0, 10)));
        }
        return token.toString();
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
