package uk.gov.justice.laa.providerdata.shared;

/** The exception thrown when item not found. */
public class ItemNotFoundException extends RuntimeException {

  /**
   * Constructor for ItemNotFoundException.
   *
   * @param message the error message
   */
  public ItemNotFoundException(String message) {
    super(message);
  }
}
