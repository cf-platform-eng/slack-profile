package hello;

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
    public String token(@RequestBody Map<String,String> input) {
        System.out.println(input);
        return input.get("challenge");

    }
}
