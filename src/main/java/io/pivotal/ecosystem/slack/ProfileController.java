package io.pivotal.ecosystem.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private String verificationToken;

    public ProfileController(SlackRepository slackRepository, String authToken, String verificationToken) {
        setSlackRepository(slackRepository);
        setAuthToken(authToken);
        setVerificationToken(verificationToken);
    }

    @PostMapping("/")
    public ResponseEntity<?> handleEvent(@RequestBody String input) {
        log.debug("input: " + input);

        //called by slack when first registering an app
        try {
            String challenge = JsonPath.parse(input).read("$.challenge");
            log.info("slack challenge: " + input);
            return ResponseEntity.ok().body(challenge);
        } catch (PathNotFoundException pe) {
            log.debug("not a slack challenge event.", pe);
        }

        //for testing, only look at changes for our test group
        Map<String, String> m;
        try {
            m = getUserInfo(getUserFromInput(input));
        } catch (Exception e) {
            log.error("error parsing response.", e);
            return ResponseEntity.ok().build();
        }
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
        try {
            String token = JsonPath.parse(input).read("$.token");
            log.debug("slack token: " + token);
            if (!getVerificationToken().equals(token)) {
                log.warn("invalid verification token.");
                return ResponseEntity.ok().build();
            }
        } catch (PathNotFoundException pe) {
            log.warn("request does not include a verification token.");
            return ResponseEntity.ok().build();
        }

        // if it's not a challenge, it's an event we are listening for...
        log.debug("ch-ch-changes: " + input);

        //don't get ourselves in a loop, only do an update if the display name needs to be changed.
        //todo look at event_time to make sure we aren't looping?
        Map<String, String> userInfo;
        try {
            userInfo = getUserInfo(getUserFromInput(input));
        } catch (Exception e) {
            log.error("error parsing response.", e);
            return ResponseEntity.ok().build();
        }

        String displayName = userInfo.get("display_name");
        String suggestedDisplayName = userInfo.get("suggestedDisplayName");

        if (!suggestedDisplayName.equals(displayName)) {
            log.info("updating display_name to: " + suggestedDisplayName);
            updateDisplayName(userInfo.get("id"), suggestedDisplayName);
        } else {
            log.info("display_name already set as suggested, no update needed.");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestedNames(@RequestParam(value = "token") String token) {
        if (!getVerificationToken().equals(token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        List<Map<String, String>> userInfos = getUserInfos();
        Map<String, Object> suggestions = new HashMap<>();
        for (Map<String, String> user : userInfos) {
            Map<String, Object> m = new HashMap<>();
            m.put("display_name", user.get("display_name"));
            m.put("suggestedDisplayName", user.get("suggestedDisplayName"));
            suggestions.put(user.get("id"), m);
        }

        return new ResponseEntity<>(suggestions, HttpStatus.OK);
    }

    @PostMapping("/bulkUpdate")
    public ResponseEntity<?> bulkUpdate(@RequestParam(value = "token") String token, @RequestParam(value = "source") String source, Map<String, Object> json) {
        if (!getVerificationToken().equals(token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        if (!"display_name".equals(source) && !"suggestedDisplayName".equals(source)) {
            return new ResponseEntity<>("source should be display_name or suggestedDisplayName", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> ret = new HashMap<>();
        for (String id : json.keySet()) {
            String name;
            if ("display_name".equals(source)) {
                name = ((Map) json.get(id)).get("display_name").toString();
            } else {
                name = ((Map) json.get(id)).get("suggestedDisplayName").toString();
            }

            updateDisplayName(id, name);
            ret.put(id, name);
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    void updateDisplayName(String id, String displayName) {
        Map<String, Object> resp = getSlackRepository().updateDisplayName(getAuthToken(), id, displayName);
        log.debug("update resp: " + resp);

        if (resp.containsKey("error")) {
            log.error("unable to process display name:" + resp);
        }
    }

    String getUserFromInput(String input) throws Exception {
        return jsonSubString((jsonSubString(input, "event")), "user");
    }

    private String jsonSubString(String json, String key) throws Exception {
        TypeReference t = new TypeReference<Map<String, Object>>() {
        };
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> m = mapper.readValue(json, t);
        return mapper.writeValueAsString(m.get(key));
    }

    Map<String, String> getUserInfo(String userJson) {

        Map<String, String> ret = new HashMap<>();

        //an id
        putIfNotNull(ret, userJson, "id", "$.id");

        //a time zone: often missing..
//        putIfNotNull(ret, userJson, "tz_label", "$.tz_label");

        //a real name
        putIfNotNull(ret, userJson, "realName", "$.profile.real_name");

        //current display name
        putIfNotNull(ret, userJson, "display_name", "$.profile.display_name");

        //do I have an email address?
        putIfNotNull(ret, userJson, "email", "$.profile.email");

        //do I have a profile.title
        putIfNotNull(ret, userJson, "title", "$.profile.title");

        //a real name normalized
        putIfNotNull(ret, userJson, "realNameNormalized", "$.profile.real_name_normalized");

        ret.put("suggestedDisplayName", constructDisplayName(ret));

        return ret;
    }

    private void putIfNotNull(Map<String, String> m, String input, String key, String jpath) {
        String s;
        try {
            s = JsonPath.parse(input).read(jpath);
        } catch (Throwable t) {
            log.info("error parsing json: key: " + key + " input: " + input);
            return;
        }
        if (s != null && s.trim().length() > 0) {
            m.put(key, s);
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

    List<Map<String, String>> getUserInfos() {
        List<Map<String, String>> userInfos = new ArrayList<>();

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("limit", 200);

        Object usersJson;

        int i = 0;
        while (i < 10) {
            usersJson = getSlackRepository().getUsers(getAuthToken(), queryMap);

            List<Object> members = JsonPath.parse(usersJson).read("$.members");
            for (Object member : members) {
                try {
                    userInfos.add(getUserInfo(new ObjectMapper().writeValueAsString(member)));
                } catch (JsonProcessingException e) {
                    log.error("error processing json input: " + member, e);
                }
            }

            String cursor = JsonPath.parse(usersJson).read("$.response_metadata.next_cursor");
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
