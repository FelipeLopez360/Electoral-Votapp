package co.com.votapp.ws.voting.application.port.out;

public interface TokenLockPort {
    boolean acquireTokenLock(String tokenId);
}
