package com.agentshield.security;

/** HTTP header names that are part of the AgentShield API contract. */
public final class ApiHeaders {

    /** Header an agent sends its raw API key in when submitting an action. */
    public static final String AGENT_API_KEY = "X-Agent-API-Key";

    private ApiHeaders() {
    }
}
