package pleasehireus.handlers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;

import pleasehireus.App;

public class OauthHandler extends Handler.Abstract {
    static record RequestData(String email) { }

    final GoogleClientSecrets secrets;
    final GoogleAuthorizationCodeFlow flow;

    public OauthHandler(GoogleClientSecrets secrets, GoogleAuthorizationCodeFlow flow) {
        this.secrets = secrets;
        this.flow = flow;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        RequestData d = new RequestData(null);
        Credential cred = d.email != null ? flow.loadCredential(d.email) : null;
        if (cred != null) {
            callback.succeeded();
        } else {
            response.write(true, ByteBuffer.wrap(flow.newAuthorizationUrl().setRedirectUri("https://jobtrack.ben.enterprises/oauthcallback").build().getBytes(StandardCharsets.UTF_8)), callback);
        }

        return true;
    }

}
