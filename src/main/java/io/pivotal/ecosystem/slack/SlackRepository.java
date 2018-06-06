package io.pivotal.ecosystem.slack;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface SlackRepository {

    @RequestLine("GET /users.list?limit=20")
    @Headers("Authorization: Bearer {token}")
    public Map<String, Object> getUsers(@Param("token") String token);
}
