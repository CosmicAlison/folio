package com.alisonpariela.blogmanager.ai;

import java.util.List;

/** Provider boundary: retrieval and ownership rules stay inside the application. */
public interface AiClient {
    List<List<Double>> embed(List<String> input);
    String generate(String system, String prompt);
}
