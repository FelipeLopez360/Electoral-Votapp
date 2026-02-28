package co.com.votapp.ws.voting.domain;

public class Token {
    private final String id;

    public Token(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Token id must not be blank");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
