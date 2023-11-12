package pleasehireus.handlers;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.gmail.Gmail;

import pleasehireus.App;
import pleasehireus.App.GmailAccountCred;
import pleasehireus.Database;
import pleasehireus.Database.Job;
import pleasehireus.Database.User;

public class OauthCallbackHandler extends Handler.Abstract {
    final GoogleClientSecrets secrets;
    final GoogleAuthorizationCodeFlow flow;

    public OauthCallbackHandler(GoogleClientSecrets secrets, GoogleAuthorizationCodeFlow flow) {
        this.secrets = secrets;
        this.flow = flow;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (request.getHttpURI().getPath().startsWith("/oauthcallback")) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            String code = URLDecoder.decode(request.getHttpURI().toString().split("\\/oauthcallback\\?code\\=")[1].split("\\&")[0], StandardCharsets.UTF_8);
            System.out.println(code);
            TokenRequest tr = flow.newTokenRequest(code).setRedirectUri("https://jobtrack.ben.enterprises/oauthcallback"); //TODO: Flaiming garbo
            TokenResponse tres = tr.execute();
            System.out.println(tres);
            Credential credential = flow.createAndStoreCredential(tres, "null");
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, App.JSON_FACTORY, credential).build();
            String email = service.users().getProfile("me").execute().getEmailAddress();
            System.out.println(email);
            flow.getCredentialDataStore().set(email, new StoredCredential(credential));
            // var messages = service.users().messages().list("me").execute().getMessages(); //.setQ("after: 1699664308")
            // for (Message m : messages) {
            //     System.out.println(service.users().messages().get(email, m.getId()).execute().getPayload().getHeaders().stream().filter(h -> "To".equals(h.getName())).findFirst().orElse(null));
            // }
            
            String tokens = App.randomID();
            App.TOKEN_2_GOOGLE_MAP.put(tokens, new GmailAccountCred(credential, email));
            System.out.println(tokens);

            Database.modifyUser(email, u -> {
                Job[] jobs = Arrays.copyOf(u.jobs(), u.jobs().length + 1);
                jobs[jobs.length - 1] = new Job("example.com", "Intern", "0", "illumina", App.randomID());
                return new User(jobs, u.emails(), u.containsEmailsUpToTimestamp());
            });

            Database.updateUserEmails(tokens, email);

            response.write(true, ByteBuffer.wrap(tokens.getBytes(StandardCharsets.UTF_8)), callback);
            return true;
        } else {
            return false;
        }
    }
    
}
