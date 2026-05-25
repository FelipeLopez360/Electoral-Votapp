package co.com.votapp.ws.voting.domain;

/**
 * Lifecycle states for a voting token.
 *
 * <p>ISSUED  → only one per funcionario+election (partial unique index)
 * USED    → token was redeemed; vote was cast
 * INVALIDATED → admin revoked the token before use
 */
public enum TokenStatus {
    ISSUED,
    USED,
    INVALIDATED
}
