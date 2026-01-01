package exception;

public class GitBenchException extends RuntimeException {

    public GitBenchException(String message) {
        super(message);
    }

    public GitBenchException(String message, Throwable cause) {
        super(message, cause);
    }
}
