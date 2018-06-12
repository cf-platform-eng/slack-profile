package io.pivotal.ecosystem.slack;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface SlackRepository {

    //need users.profile.write scope
    @RequestLine("POST /users.profile.set?user={id}&name=display_name&value={displayName}")
    @Headers({"Authorization: Bearer {token}"})
    public Map<String, Object> updateDisplayName(@Param("token") String token, @Param("id") String id, @Param("displayName") String displayName);
}
