package io.pivotal.ecosystem.slack;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.StreamUtils;

import javax.ws.rs.core.MediaType;
import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * to run this test, edit application.properties and add in your slack tokens.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Ignore
public class ProfileRestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private String verificationToken;

    @Test
    public void testGetSuggestedNames() throws Exception {
        ResultActions ra = this.mockMvc.perform(get("/suggestions?token=" + verificationToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        assertNotNull(ra.andReturn().getResponse().getContentAsString());
    }

    @Test
    public void testBulkUpdate() throws Exception {
        String names = getContents("names.json");
        ResultActions ra = mockMvc.perform(post("/bulkUpdate?token=" + verificationToken + "&name_source=suggestedDisplayName")
                .content(names)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        assertNotNull(ra.andReturn().getResponse().getContentAsString());
    }

    private String getContents(String fileName) throws Exception {
        return StreamUtils.copyToString(new ClassPathResource(fileName).getInputStream(), Charset.defaultCharset());
    }
}
