package com.nicapz.gym.Functions;

import org.springframework.stereotype.Component;

@Component
public class UserContext {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public void setUserId(String userId) {
        currentUser.set(userId);
    }

    public String getUserId() {
        return currentUser.get();
    }

    public void setUserSessionId(String userSessionId) {
        currentUser.set(userSessionId);
    }

    public String getUserSessionId() {
        return currentUser.get();
    }

    public void clear() {
        currentUser.remove();
    }


}
