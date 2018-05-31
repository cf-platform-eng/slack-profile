package hello;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
public class HelloController {
    
    @RequestMapping("/")
    public String index() {
        return "Greetings from Space!";
    }

    @RequestMapping(method = RequestMethod.POST, path="/")
    public Map<String, String> token(@RequestBody Map<String,String> input) {
        System.out.println(input.get("challenge"));
        input.remove("token");
        input.remove("type");
        return input;

    }
}
