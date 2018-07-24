package io.pivotal.ecosystem.slack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ProfileControllerTest {

    @Autowired
    private ProfileController profileController;

    @Autowired
    private String verificationToken;

    @Test
    public void testGetUserFromInput() throws Exception {
        String s = profileController.getUserFromInput(getContents("event.json"));
        assertNotNull(s);
        Map<String, String> user = profileController.getUserInfo(s);
        assertNotNull(user);
        assertEquals(7, user.size());
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
        Map<String, Object> m = profileController.getSuggestedNames(verificationToken).getBody();
        assertNotNull(m);
        assertTrue(m.size() > 0);

        log.debug("unique users: " + m.size());
        for (String s : m.keySet()) {
            log.debug(s);
        }
    }

    @Test
    public void testBulkUpdate() throws Exception {
        String names = getContents("names.json");
        TypeReference t = new TypeReference<Map<String, Object>>() {};
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> m = mapper.readValue(names, t);
        Object ret = profileController.bulkUpdate(verificationToken, "suggestedDisplayName", m).getBody();
        assertNotNull(ret);
    }

    private String getContents(String fileName) throws Exception {
        return StreamUtils.copyToString(new ClassPathResource(fileName).getInputStream(), Charset.defaultCharset());
    }
}
