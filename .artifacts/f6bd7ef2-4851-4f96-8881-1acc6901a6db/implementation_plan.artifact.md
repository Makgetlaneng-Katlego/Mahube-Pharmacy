# Implementation Plan - Remove Password Hashing

This plan outlines the steps to remove BCrypt password hashing from the Mahube Pharmacy application, reverting to plain-text password storage and verification.

> [!CAUTION]
> **Security Warning**: Removing password hashing means user passwords will be stored as plain text in your database. If the database is ever compromised, all user passwords will be exposed. This is generally discouraged in production environments.

## Proposed Changes

### Database Layer

#### [MODIFY] [DBConnection.kt](file:///C:/Users/makge/AndroidStudioProjects/MahubePhamarcy/app/src/main/java/com/example/mahubephamarcy/DBConnection.kt)

- **Remove BCrypt Import**: Remove `import org.mindrot.jbcrypt.BCrypt`.
- **Modify `insertUserTable`**: Remove the hashing logic and save the `pass` parameter directly to the database.
- **Modify `verifyUser`**:
    - Remove the prefix normalization logic.
    - Replace the `BCrypt.checkpw` call with a direct string comparison (`pass == storedPassword`).

## Verification Plan

### Automated Tests
- None planned as this is a functional logic change.

### Manual Verification
1. **Create New User**: Register a new user in the app.
2. **Verify Database**: Check the database directly to confirm the password is saved in plain text.
3. **Login Test**: Attempt to log in with the new plain-text credentials.
4. **Existing Users**: Note that users created *before* this change (with hashed passwords) will no longer be able to log in, as the app will now be comparing their plain-text entry to a hashed string in the database.
