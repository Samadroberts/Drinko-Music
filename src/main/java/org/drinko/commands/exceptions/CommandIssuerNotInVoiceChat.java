package org.drinko.commands.exceptions;

public class CommandIssuerNotInVoiceChat extends Exception {
    public CommandIssuerNotInVoiceChat() {
        super("You must join a voice channel first.");
    }
}
