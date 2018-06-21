package io.pivotal.ecosystem.slack;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
@Data
public class ProfileController {

    private SlackRepository slackRepository;
    private String authToken;
    private String verificationToken;

    public ProfileController(SlackRepository slackRepository, String authToken, String verificationToken) {
        setSlackRepository(slackRepository);
        setAuthToken(authToken);
        setVerificationToken(verificationToken);
    }

    @PostMapping("/")
    public ResponseEntity<?> handleEvent(@RequestBody Map<String, Object> input) {
        log.debug("token: " + input.get("token"));
        log.info("input: " + input);

        //called by slack when first registering an app
        if (input.containsKey("challenge")) {
            log.info("slack challenge: " + input);
            input.remove("token");
            input.remove("type");
            return ResponseEntity.ok().body(input);
        }

        Map<String, String> m = getUserInfoFromEvent(input);
        String id = m.get("id");
        log.info("who? " + m);
        if (
                !"U1N7LKZ1U".equals(id) &&
                        !"UB8CU9E7L".equals(id) &&
                        !"UB1BW707P".equals(id) &&
                        !"U89FUEYR2".equals(id) &&
                        !"UB8CU9E7L".equals((id))
                ) {
            return ResponseEntity.ok().body(input);
        }

        //is this request really from slack?
        if ((!input.containsKey("token")) || (!input.get("token").equals(getVerificationToken()))) {
            log.warn("request does not contain a valid verification token.");
            return ResponseEntity.ok().build();
        }

        // if it's not a challenge, it's an event we are listening for...
        log.debug("ch-ch-changes: " + input);

        //don't get ourselves in a loop, only do an update if the display name needs to be changed.
        //todo look at event_time to make sure we aren't looping?

        Map<String, String> userInfo = getUserInfox(input);
        String displayName = userInfo.get("display_name");
        String suggestedDisplayName = constructDisplayName(userInfo);

        if (!suggestedDisplayName.equals(displayName)) {
            log.info("updating display_name to: " + suggestedDisplayName);
            updateDisplayName(userInfo.get("id"), suggestedDisplayName);
        } else {
            log.info("display_name already set as suggested, no update needed.");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/suggestions")
    public ResponseEntity<TreeSet<String>> getSuggestedNames(@RequestParam(value="token") String token) {
        if(token == null || ! getVerificationToken().equals(token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        List<Map<String, String>> userInfos = getUserInfos();
        TreeSet<String> set = new TreeSet<>();
        for (Map<String, String> user : userInfos) {
            set.add(user.get("suggestedDisplayName"));
        }

        return new ResponseEntity<>(set, HttpStatus.OK);
    }

    void updateDisplayName(String id, String displayName) {
        Map<String, Object> resp = getSlackRepository().updateDisplayName(getAuthToken(), id, displayName);
        log.debug("update resp: " + resp);

        if (resp.containsKey("error")) {
            log.error("unable to process display name:" + resp);
        }
    }

    //todo jsonpath?
    @SuppressWarnings("unchecked")
    Map<String, String> getUserInfoFromEvent(Map<String, Object> input) {

        Map<String, String> ret = new HashMap<>();
        if (!input.containsKey("event")) {
            return ret;
        }

        Map<String, Object> event = (Map<String, Object>) input.get("event");
        if (!event.containsKey("user")) {
            return ret;
        }

        Map<String, Object> user = (Map<String, Object>) event.get("user");

        return getUserInfox(user);
    }

    //todo jsonpath?
    @SuppressWarnings("unchecked")
    Map<String, String> getUserInfox(Map<String, Object> user) {

        Map<String, String> ret = new HashMap<>();

        //an id
        putIfNotNull(ret, "id", user.get("id"));

        //a time zone
        putIfNotNull(ret, "tz", user.get("tz_label"));

        //a real name
        putIfNotNull(ret, "realName", user.get("real_name"));

        //profile
        if (user.containsKey("profile")) {
            Map<String, Object> profile = (Map<String, Object>) user.get("profile");

            //do I have an email address?
            putIfNotNull(ret, "email", profile.get("email"));

            //do I have a profile.title
            putIfNotNull(ret, "title", profile.get("title"));

            //a real name normalized
            putIfNotNull(ret, "realNameNormalized", profile.get("real_name_normalized"));
        }

        ret.put("suggestedDisplayName", constructDisplayName(ret));

        return ret;
    }

    private void putIfNotNull(Map<String, String> m, String key, Object o) {
        if (o != null && o.toString().trim().length() > 0) {
            m.put(key, o.toString());
        }
    }

    String constructDisplayName(Map<String, String> userInfo) {
        if (userInfo == null) {
            return null;
        }

        //ideal name would be "realNameNormalized title company tz" but we can only fit three of these things in...
        String ret;
        if (userInfo.containsKey("realNameNormalized")) {
            ret = WordUtils.capitalize(userInfo.get("realNameNormalized").trim());
        } else if (userInfo.containsKey("realName")) {
            ret = WordUtils.capitalize(userInfo.get("realName").trim());
        } else {
            ret = userInfo.get("displayName");
        }

        String company = getCompanyFromEmail(userInfo);
        //"gmail" is not a company...
        if (company != null && (!company.toUpperCase().equals("GMAIL"))) {
            ret += ", " + WordUtils.capitalize(company.trim());
        } else {
            //add the user's title instead?
            if (userInfo.containsKey("title")) {
                ret += ", " + WordUtils.capitalize(userInfo.get("title").trim());
            }
        }

        return ret;
    }

    String getCompanyFromEmail(Map<String, String> userInfo) {
        if (userInfo == null || !userInfo.containsKey("email")) {
            return null;
        }
        String email = userInfo.get("email");

        EmailValidator validator = EmailValidator.getInstance();
        if (!validator.isValid(email)) {
            return null;// is valid, do something
        }

        email = email.substring(email.indexOf('@') + 1, email.length());
        return email.substring(0, email.indexOf("."));
    }

    public List<Map<String, String>> getUserInfos() {
        List<Map<String, String>> userInfos = new ArrayList<>();

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("limit", 200);

        Map<String, Object> users;

        int i = 0;
        while (i < 10) {
            users = getSlackRepository().getUsers(getAuthToken(), queryMap);
            List<Object> members = (List<Object>) users.get("members");
            for (Object member : members) {
                userInfos.add(getUserInfox((Map<String, Object>) member));
            }

            //result will be paged, need to grab the cursor from the response and spin through rest of the pages
            Map<String, Object> metatdata = (Map<String, Object>) users.get("response_metadata");
            if (metatdata == null) {
                break;
            }

            String cursor = metatdata.get("next_cursor").toString();
            if (cursor == null || cursor.length() < 1) {
                break;
            }

            queryMap.put("cursor", cursor);
            i++;
        }

        log.info("iterated: " + i);
        return userInfos;
    }
}
