# Walkthrough - Fixed `findNavController` Build Error

I have resolved the Kotlin compilation error in `ProfileFragment.kt`.

## Changes Made

### UI - Profile
#### [ProfileFragment.kt](file:///D:/AndroidStudioProjects/PetCare/app/src/main/java/com/rijana/petcare/ui/profile/ProfileFragment.kt)
- Fixed the `logOut()` function which was causing a "Too many arguments" error during build.
- Switched from the ambiguous extension function call to the explicit `Navigation.findNavController(Activity, ViewId)` utility method. This ensures the app correctly finds the outer navigation controller for the Activity when logging out.

## Verification Results

### Automated Tests
- Executed `./gradlew :app:compileDebugKotlin`
- **Result:** Build finished successfully.

```text
BUILD SUCCESSFUL in 2s
```
