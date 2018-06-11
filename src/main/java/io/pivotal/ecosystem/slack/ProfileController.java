package io.pivotal.ecosystem.slack;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Data
public class ProfileController {

    private SlackRepository slackRepository;
    private String authToken;

    public ProfileController(SlackRepository slackRepository, String authToken) {
        setSlackRepository(slackRepository);
        setAuthToken(authToken);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/")
    public Map<String, Object> token(@RequestBody Map<String, Object> input) {

        if (input != null && input.containsKey("challenge")) {
            log.info(input.get("challenge").toString());
            input.remove("token");
            input.remove("type");
            return input;
        }

        // assume this is event info
        log.info("got a change!!!! " + input);
        return null;
    }

    @RequestMapping("/users")
    public List<Object> getUsers() {
        Map<String, Object> users = slackRepository.getUsers(getAuthToken());
        if (!users.containsKey("members")) {
            return new ArrayList<>();
        }

        return (List) users.get("members");
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return slackRepository.getUserInfo(getAuthToken(), id);
    }

    @PostMapping("/user/{id}")
    public ResponseEntity<?> updateDisplayName(@PathVariable String id, @RequestBody String displayName) {
        String s;
        try {
            s = URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("error processing display name.", e);
            return null;
        }

        Map<String, Object> resp = slackRepository.updateDisplayName(getAuthToken(), id, s);
        if ((!resp.containsKey(("ok")) || resp.get("ok").equals(Boolean.FALSE))) {
            log.error("unable to process display name.", resp.get("error"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok().build();
    }


    @RequestMapping("/userInfo/{id}")
    public Map<String, String> getUserInfo(@PathVariable String id) {
        Map<String, Object> info = getUser(id);
        Map<String, String> ret = new HashMap<>();

        if (info.containsKey("error")) {
            ret.put(id, info.get("error").toString());
            return ret;
        }

        Object userInfo = info.get("user");
        if (userInfo == null) {
            ret.put(id, "user information not available.");
            return ret;
        }

        Map<String, Object> userMap = (Map<String, Object>) userInfo;
        log.info("user: " + userInfo);

        //a time zone
        putIfNotNull(ret, "tz", userMap.get("tz"));

        //a real name
        putIfNotNull(ret, "realName", userMap.get("real_name"));

        //profile
        Object profile = userMap.get("profile");

        if (profile != null) {
            Map<String, Object> profileMap = (Map<String, Object>) profile;

            //do I have an email address?
            putIfNotNull(ret, "email", profileMap.get("email"));

            //do I have a profile.title
            putIfNotNull(ret, "title", profileMap.get("title"));

            //a real name normalized
            putIfNotNull(ret, "realNameNormalized", profileMap.get("real_name_normalized"));
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

        //ideal name would be "realNameNormalized: title: company: tz"

        String ret;
        if (userInfo.containsKey("realNameNormalized")) {
            ret = WordUtils.capitalize(userInfo.get("realNameNormalized").trim());
        } else if (userInfo.containsKey("realName")) {
            ret = WordUtils.capitalize(userInfo.get("realName").trim());
        } else {
            ret = userInfo.get("displayName");
        }

        if (userInfo.containsKey("title")) {
            ret += ": " + WordUtils.capitalize(userInfo.get("title").trim());
        }

        String company = getCompanyFromEmail(userInfo);
        if (company != null) {
            ret += ": " + WordUtils.capitalize(company.trim());
        }

        if (userInfo.containsKey("tz")) {
            ret += ": " + userInfo.get("tz");
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
