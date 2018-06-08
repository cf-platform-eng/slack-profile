package io.pivotal.ecosystem.slack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProfileControllerTest {

    @Autowired
    private ProfileController profileController;

    @Test
    public void testGetUsers() {
        List<Object> users = profileController.getUsers();
        assertNotNull(users);
        assertTrue(users.size() > 0);
    }

    @Test
    public void testGetUser() {
        Map<String, Object> user = profileController.getUser("UAZK3F2UV");
        assertNotNull(user);
        assertTrue(user.size() > 0);
    }

    @Test
    public void testGetUserInfo() {
        Map<String, String> user = profileController.getUserInfo("UAZK3F2UV");
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
        assertEquals("Foo Bar: mars", profileController.constructDisplayName(userInfo));

        userInfo.put("title", "poobah");
        assertEquals("Foo Bar: Poobah: mars", profileController.constructDisplayName(userInfo));

        userInfo.put("email", "foo.bar@bazz.com");
        assertEquals("Foo Bar: Poobah: Bazz: mars", profileController.constructDisplayName(userInfo));
    }
}
