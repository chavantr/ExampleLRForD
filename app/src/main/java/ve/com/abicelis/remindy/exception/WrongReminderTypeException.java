package ve.com.abicelis.remindy.exception;



public class WrongReminderTypeException extends Exception {

    private static final String DEFAULT_MESSAGE = "Wrong reminder type.";

    public WrongReminderTypeException() {
        super(DEFAULT_MESSAGE);
    }
    public WrongReminderTypeException(String message) {
        super(message);
    }
    public WrongReminderTypeException(String message, Throwable cause) {
        super(message, cause);
    }

}
