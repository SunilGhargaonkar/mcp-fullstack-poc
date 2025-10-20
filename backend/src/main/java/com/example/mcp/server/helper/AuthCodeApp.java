package com.example.mcp.server.helper;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;

public class AuthCodeApp extends AuthorizationCodeInstalledApp {
    public AuthCodeApp(GoogleAuthorizationCodeFlow flow) {
        super(flow, new LocalServerReceiver.Builder().setPort(8888).build());
    }
}
