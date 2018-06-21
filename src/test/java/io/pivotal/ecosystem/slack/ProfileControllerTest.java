package io.pivotal.ecosystem.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.springframework.util.ResourceUtils.getFile;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ProfileControllerTest {

    @Autowired
    private ProfileController profileController;

    @Autowired
    private String verificationToken;

    @Test
    public void testGetUserInfo() throws IOException {
        Map<String, String> user = profileController.getUserInfoFromEvent(fromJson("src/test/resources/event.json"));
        assertNotNull(user);
        assertTrue(user.size() > 0);
    }

    @Test
    public void testGetCompanyFromEmail() {
        assertNull(profileController.getCompanyFromEmail(null));

        Map<String, String> userInfo = new HashMap<>();
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("foo", "bar");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "bar");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", ".");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "@");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", null);
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "@.");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", ".@");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jgordon@");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jgordon@pivotal");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jgordon.pivotal@io");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jared.gordon@pivotal@io");
        assertNull(profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jared.gordon@pivotal.io");
        assertEquals("pivotal", profileController.getCompanyFromEmail(userInfo));

        userInfo.put("email", "jgordon@pivotal.io");
        assertEquals("pivotal", profileController.getCompanyFromEmail(userInfo));
    }

    @Test
    public void testconstructDisplayName() {
        assertNull(profileController.constructDisplayName(null));

        Map<String, String> userInfo = new HashMap<>();
        assertNull(profileController.constructDisplayName(userInfo));

        userInfo.put("displayName", null);
        assertNull(profileController.constructDisplayName(userInfo));

        userInfo.put("displayName", "foo one");
        assertEquals("foo one", profileController.constructDisplayName(userInfo));

        userInfo.put("realName", "foo Two");
        assertEquals("Foo Two", profileController.constructDisplayName(userInfo));

        userInfo.put("realNameNormalized", "foo bar");
        assertEquals("Foo Bar", profileController.constructDisplayName(userInfo));

        userInfo.put("tz", "mars");
        assertEquals("Foo Bar", profileController.constructDisplayName(userInfo));

        userInfo.put("title", "poobah");
        assertEquals("Foo Bar, Poobah", profileController.constructDisplayName(userInfo));

        userInfo.put("email", "foo.bar@bazz.com");
        assertEquals("Foo Bar, Bazz", profileController.constructDisplayName(userInfo));
    }

    @Test
    public void testGetSuggestedNames() {
        TreeSet<String> set = profileController.getSuggestedNames(verificationToken).getBody();
        assertNotNull(set);
        assertTrue(set.size() > 0);

        log.debug("unique users: " + set.size());
        for (String s : set) {
            log.debug(s);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String fileName) throws IOException {
        return (Map<String, Object>) new ObjectMapper().readValue(getFile(fileName), HashMap.class);
    }
}
