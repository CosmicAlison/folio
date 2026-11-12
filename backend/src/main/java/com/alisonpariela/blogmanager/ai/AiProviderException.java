package com.alisonpariela.blogmanager.ai;

/** Only safe, application-owned messages may cross the HTTP boundary. */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) {
        super(message);
    }
}
