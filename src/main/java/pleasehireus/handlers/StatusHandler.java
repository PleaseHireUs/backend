package pleasehireus.handlers;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.google.common.html.HtmlEscapers;

import pleasehireus.App;
import pleasehireus.Database;
import pleasehireus.Database.Email;
import pleasehireus.Database.Job;
import pleasehireus.Database.User;

public class StatusHandler extends Handler.Abstract  {
    static final String template;

    static {
        try (BufferedReader r = Files.newBufferedReader(Paths.get("./template.html"), StandardCharsets.UTF_8)) {
            template = r.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        // ByteArrayOutputStream os = new ByteArrayOutputStream();
        // OutputStreamWriter w = new OutputStreamWriter(os);
        String email_addy = App.TOKEN_2_GOOGLE_MAP.get(
            request.getHttpURI().toString().split("/status/")[1]
            ).email();
        User u = Database.email2user.get(
            email_addy
        );
        StringBuilder builder = new StringBuilder();
        HashMap<String, List<Email>> emails = new HashMap<>();
        HashMap<String, Long> jobLastTouched = new HashMap<>();
        for (Job j : u.jobs()) {
            jobLastTouched.put(j.internalJobId(), Long.valueOf(j.time()));
        }
        for (Email e : u.emails()) {
            emails.computeIfAbsent(e.internalJobId(), l -> new ArrayList<>()).add(e);
            jobLastTouched.compute(e.internalJobId(), (k, v) -> Math.max(v, e.timestamp()));
        }
        for (List<Email> emails2 : emails.values()) {
            emails2.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        }
        ArrayList<Job> jobs = new ArrayList<>(Arrays.asList(u.jobs()));
        jobs.sort((a, b) -> Long.compare(jobLastTouched.get(b.internalJobId()), jobLastTouched.get(a.internalJobId())));

        var esc = HtmlEscapers.htmlEscaper();

        for (Job j : jobs) {
            if ("Rejected".equals(j.status())) {
                builder.append("<div class=\"hero reject\">");
            } else {
                builder.append("<div class=\"hero\">");
            }
            
            builder.append("<hgroup>");
            builder.append("<h3>");
            builder.append(esc.escape(j.company()));
            builder.append("</h3>");
            builder.append("<h4>");
            builder.append(esc.escape(j.position()));
            builder.append("</h4>");
            builder.append("</hgroup>");
            builder.append("Applied " + daysAgo(Long.parseLong(j.time())) + " days ago");
            builder.append("<br />");
            builder.append("Status: " + j.status());
            builder.append("<br />");
            if (emails.containsKey(j.internalJobId())) {
                builder.append("<br />");
                builder.append("Recent Updates: ");
                builder.append("<ul>");
                for (Email e : emails.get(j.internalJobId())) {
                    builder.append("<li>");
                    builder.append("<a href=\"https://mail.google.com/mail/u/" + email_addy + "/#all/" + e.id() + "\" target=\"_blank\" rel=\"noopener noreferrer\">");
                    builder.append(esc.escape(e.subject()));
                    builder.append("</a>");
                    builder.append(" (" + daysAgo(e.timestamp()) + " days ago)");
                    builder.append("</li>");
                }
                builder.append("</ul>");
            }
            builder.append("</div>");
        }
        String r = template.replace("$$GEN$$", builder);
        response.write(true, ByteBuffer.wrap(r.getBytes(StandardCharsets.UTF_8)), callback);
        return true;
    }

    static long daysAgo(long unixMilis) {
        return (System.currentTimeMillis() - unixMilis) / (1000 * 60 * 60 * 24);
    }
    
}
