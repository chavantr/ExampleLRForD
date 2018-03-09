package ve.com.abicelis.remindy.exception;



public class PlaceNotFoundException extends Exception {

    private static final String DEFAULT_MESSAGE = "Place not found in the database.";

    public PlaceNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
    public PlaceNotFoundException(String message) {
        super(message);
    }
    public PlaceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
