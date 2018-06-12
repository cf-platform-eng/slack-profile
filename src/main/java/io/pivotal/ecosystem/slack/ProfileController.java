package io.pivotal.ecosystem.slack;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

        //is this request really from slack?
        if ((!input.containsKey("token")) || (!input.get("token").equals(getVerificationToken()))) {
            log.warn("request does not contain a valid verification token.");
            return ResponseEntity.ok().build();
        }

        //called by slack when first registering an app
        if (input.containsKey("challenge")) {
            input.remove("token");
            input.remove("type");
            return ResponseEntity.ok().body(input);
        }

        // if it's not a challenge, it's an event we are listening for...
        log.debug("ch-ch-ch-changes: " + input);

        //don't get ourselves in a loop, only do an update if the display name needs to be changed.
        //todo look at event_time to make sure we aren't looping?

        Map<String, String> userInfo = getUserInfo(input);
        String displayName = userInfo.get("display_name");
        String suggestedDisplayName = constructDisplayName(userInfo);

        if (!suggestedDisplayName.equals(displayName)) {
            updateDisplayName(userInfo.get("id"), suggestedDisplayName);
        } else {
            log.info("display_name already set as suggested, no update needed.");
        }
        return ResponseEntity.ok().build();
    }

    private void updateDisplayName(@PathVariable String id, @RequestBody String displayName) {
        Map<String, Object> resp = slackRepository.updateDisplayName(getAuthToken(), id, displayName);
        if (resp.containsKey("error")) {
            log.error("unable to process display name.", resp);
        }
    }

    //todo jsonpath?
    @SuppressWarnings("unchecked")
    Map<String, String> getUserInfo(Map<String, Object> input) {

        Map<String, String> ret = new HashMap<>();
        if (!input.containsKey("event")) {
            return ret;
        }

        Map<String, Object> event = (Map<String, Object>) input.get("event");
        if (!event.containsKey("user")) {
            return ret;
        }

        Map<String, Object> user = (Map<String, Object>) event.get("user");

        //a time zone
        putIfNotNull(ret, "tz", user.get("tz"));

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

        //ideal name would be "realNameNormalized title company tz"
        String ret;
        if (userInfo.containsKey("realNameNormalized")) {
            ret = WordUtils.capitalize(userInfo.get("realNameNormalized").trim());
        } else if (userInfo.containsKey("realName")) {
            ret = WordUtils.capitalize(userInfo.get("realName").trim());
        } else {
            ret = userInfo.get("displayName");
        }

        if (userInfo.containsKey("title")) {
            ret += " " + WordUtils.capitalize(userInfo.get("title").trim());
        }

        String company = getCompanyFromEmail(userInfo);
        if (company != null) {
            ret += " " + WordUtils.capitalize(company.trim());
        }

        if (userInfo.containsKey("tz")) {
            ret += " " + userInfo.get("tz");
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
}
