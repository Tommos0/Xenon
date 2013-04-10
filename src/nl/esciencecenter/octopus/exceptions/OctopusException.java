package nl.esciencecenter.octopus.exceptions;

public class OctopusException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String adaptorName;

    public OctopusException(String adaptorName, String message) {
        super(message);
        this.adaptorName = adaptorName;
    }

    public OctopusException(String adaptorName, String message, Throwable t) {
        super(message, t);
        this.adaptorName = adaptorName;
    }

    @Override
    public String getMessage() {
        String result = super.getMessage();
        if (adaptorName != null) {
            result = adaptorName + " adaptor: " + result;
        }

        return result;
    }
}
