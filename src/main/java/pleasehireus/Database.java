package pleasehireus;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class Database {
    public static ConcurrentHashMap<String, User> email2user = new ConcurrentHashMap<>();
    public static record User(Job[] jobs, Email[] emails, long containsEmailsUpToTimestamp) { }
    public static record Job(String url, String position, String time, String company, String internalJobId, String status) { }
    public static record Email(String internalJobId, String id, /* nullable */ String subject, long timestamp) { }

    public static void modifyUser(String email, Function<User, User> func) {
        email2user.compute(email, (k, e) -> {
            if (e == null) e = new User(new Job[0], new Email[0], 0l);
            return func.apply(e);
        });

        //todo save
    }

    public static void updateUserEmails(String token, String email) {
        modifyUser(email, user -> {
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                Gmail service = new Gmail.Builder(HTTP_TRANSPORT, App.JSON_FACTORY, App.TOKEN_2_GOOGLE_MAP.get(token).credential()).build();
                long containsEmailsUpToTimestamp = user.containsEmailsUpToTimestamp();
                HashSet<String> seenEmails = new HashSet<>(user.emails().length);
                ArrayList<Email> emails = new ArrayList<>(Arrays.asList(user.emails));
                ArrayList<Job> jobs = new ArrayList<>(Arrays.asList(user.jobs));
                for (Email e : user.emails()) {
                    seenEmails.add(e.id);
                }
                var responseList = service.users().messages().list("me").setQ("{" + Arrays.stream(user.jobs).map(Job::company).collect(Collectors.joining(" ")) + "}").execute();
                
                for (;;) {
                    var messages = responseList.getMessages();
                    if (messages == null) break;
                    for (Message m_min : messages) {
                        if (seenEmails.contains(m_min.getId())) continue;
                        Message m = (service.users().messages().get("me", m_min.getId()).setFormat("raw")).execute();
                        Message m2 = (service.users().messages().get("me", m_min.getId()).setFormat("metadata")).execute();
                        containsEmailsUpToTimestamp = Math.max(containsEmailsUpToTimestamp, m.getInternalDate());
                        // https://stackoverflow.com/a/37490978
                        String messageData = StringUtils.newStringUtf8(   Base64.decodeBase64 (m.getRaw()));
                        for (int i = 0; i < jobs.size(); i++) {
                            Job job = jobs.get(i);
                            if (containsIgnoreCase(messageData, job.company()) && !(containsIgnoreCase(messageData, "Posted Today") && containsIgnoreCase(messageData, "swelist")) /*|| m.getPayload().getHeaders().stream().filter(h -> containsIgnoreCase(h.getValue(), job.company())).findAny().isPresent()*/) {
                                emails.add(new Email(job.internalJobId, m.getId(), m2.getPayload().getHeaders().stream().filter(h -> h.getName().equals("Subject")).map(MessagePartHeader::getValue).findFirst().orElse("[No Subject]"), m.getInternalDate()));
                                // System.out.println(m.getPayload().getBody().getData());
                                // System.out.println(StringUtils.newStringUtf8(   Base64.decodeBase64 (m.getRaw())));
                                if (containsIgnoreCase(messageData, "will not") ||
                                    containsIgnoreCase(messageData, "unfortunately") ||
                                    containsIgnoreCase(messageData, "other candidate")
                                ) {
                                    jobs.set(i, new Job(job.url, job.position, job.time, job.company, job.internalJobId, "Rejected"));
                                }
                                break;
                            }
                        }
                        // System.out.println.getHeaders().stream().filter(h -> "To".equals(h.getName())).findFirst().orElse(null));
                    }
                    if (responseList.getNextPageToken() != null) {
                        responseList = service.users().messages().list("me").setPageToken(responseList.getNextPageToken()).execute();
                    } else {
                        break;
                    }
                }
                return new User(jobs.toArray(new Job[0]), emails.toArray(new Email[0]), containsEmailsUpToTimestamp);
            } catch (IOException e) {
                throw new UncheckedExecutionException(e);
            } catch (GeneralSecurityException e1) {
                throw new RuntimeException(e1);
            }
        });
    }

    
    public static boolean containsIgnoreCase(String str, String searchStr)     {
        https://stackoverflow.com/a/14018549
        if(str == null || searchStr == null) return false;
    
        final int length = searchStr.length();
        if (length == 0)
            return true;
    
        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }
    

    // public static 

    //todo read
}
