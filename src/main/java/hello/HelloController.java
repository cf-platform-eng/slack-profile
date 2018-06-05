package hello;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class HelloController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Space!";
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
        else {
            log.info("got a change!!!! " + input.toString());
            return null;
        }

    }
}
