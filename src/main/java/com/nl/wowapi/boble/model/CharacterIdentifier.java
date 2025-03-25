package com.nl.wowapi.boble.model;

public record CharacterIdentifier(String realm, String name) {
    public String getRealm() {
        return realm;
    }

    public String getName() {
        return name;
    }
}
