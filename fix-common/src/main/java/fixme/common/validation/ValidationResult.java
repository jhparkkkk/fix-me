package fixme.common.validation;

/**
 * Result of message validation.
 * Immutable value object representing success or failure.
 * 
 * Design Pattern: Value Object
 */
public class ValidationResult {
    
    private final boolean valid;
    private final String errorMessage;
    
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Create a successful validation result.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }
    
    /**
     * Create a failed validation result with error message.
     */
    public static ValidationResult fail(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        return new ValidationResult(false, errorMessage);
    }
    
    /**
     * Check if validation was successful.
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Get error message (null if validation succeeded).
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Throw exception if validation failed.
     * Useful for fail-fast error handling.
     */
    public void throwIfInvalid() {
        if (!valid) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    @Override
    public String toString() {
        return valid ? "ValidationResult[VALID]" : 
                      "ValidationResult[INVALID: " + errorMessage + "]";
    }
}