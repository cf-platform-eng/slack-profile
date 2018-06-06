package io.pivotal.ecosystem.slack;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
}
