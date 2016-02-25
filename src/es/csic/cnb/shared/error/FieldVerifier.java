package es.csic.cnb.shared.error;

/**
 * <p>
 * FieldVerifier validates that the name the user enters is valid.
 * </p>
 * <p>
 * This class is in the <code>shared</code> package because we use it in both
 * the client code and on the server. On the client, we verify that the name is
 * valid before sending an RPC request so the user doesn't have to wait for a
 * network round trip to get feedback. On the server, we verify that the name is
 * correct to ensure that the input is correct regardless of where the RPC
 * originates.
 * </p>
 * <p>
 * When creating a class that is used on both the client and the server, be sure
 * that all code is translatable and does not use native JavaScript. Code that
 * is not translatable (such as code that interacts with a database or the file
 * system) cannot be compiled into client side JavaScript. Code that uses native
 * JavaScript (such as Widgets) cannot be run on the server.
 * </p>
 */
public class FieldVerifier {

//  /**
//   * Verifies that the specified email address has the proper syntax.
//   *
//   * @param mail the email address to validate
//   * @return true if valid or null, false if invalid
//   */
//  public static boolean isValidMail(String mail) {
//    if (mail == null) {
//      return false;
//    }
//    return mail.matches("^([a-zA-Z0-9_.\\-+])+@(([a-zA-Z0-9\\-])+\\.)+[a-zA-Z0-9]{2,4}$");
//  }

  /**
   * Verifies that exists a model file
   *
   * @param id the hidden field with the target file id
   * @return true if valid, false if invalid
   */
  public static boolean isModelUploaded(int num) {
    return (num > 1);
  }
}
