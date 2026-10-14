# Walkthrough - Password Hashing Removal

I have removed the BCrypt password hashing logic from the application. User passwords are now stored and verified in plain text.

> [!CAUTION]
> **Security Reminder**: The application now stores passwords in plain text. This is highly insecure and should only be used for debugging or non-production environments.

## Changes Made

### Database Layer

#### [DBConnection.kt](file:///C:/Users/makge/AndroidStudioProjects/MahubePhamarcy/app/src/main/java/com/example/mahubephamarcy/DBConnection.kt)
- **Removed `jbcrypt`**: Deleted the `import org.mindrot.jbcrypt.BCrypt` line.
- **Updated `insertUserTable`**: The password is now passed directly to the `INSERT` statement without hashing.
- **Updated `verifyUser`**: Replaced the BCrypt verification logic and prefix normalization with a direct string comparison (`pass == storedHash`).

## Verification Results

### Functionality
- The code now performs a direct equality check between the entered password and the database value.
- New users will have their passwords saved exactly as entered.

### Compatibility Warning
- **Important**: Any users created while hashing was active will no longer be able to log in, because their stored password is a BCrypt hash, but the app will now be looking for a plain text match. You may need to reset or recreate those users in your database.

render_diffs(file:///C:/Users/makge/AndroidStudioProjects/MahubePhamarcy/app/src/main/java/com/example/mahubephamarcy/DBConnection.kt)
