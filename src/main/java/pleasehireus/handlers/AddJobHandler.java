package pleasehireus.handlers;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.google.gson.Gson;

import pleasehireus.App;
import pleasehireus.App.GmailAccountCred;
import pleasehireus.Database;
import pleasehireus.Database.Job;
import pleasehireus.Database.User;

public class AddJobHandler extends Handler.Abstract {
    static record RequestData(String token, String url, String position, String company, String time) { }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        RequestData rd;
        try (InputStreamReader r = new InputStreamReader(Content.Source.asInputStream(request), StandardCharsets.UTF_8)) {
            rd = new Gson().fromJson(r, RequestData.class);
        }
        GmailAccountCred c = App.TOKEN_2_GOOGLE_MAP.get(rd.token);
        Database.modifyUser(c.email(), u -> {
            Job[] jobs = Arrays.copyOf(u.jobs(), u.jobs().length + 1);
            jobs[jobs.length - 1] = new Job(rd.url, rd.position, rd.time, rd.company, App.randomID());
            return new User(jobs, u.emails(), u.containsEmailsUpToTimestamp());
        });
        callback.succeeded();
        return true;
    }
    
}
