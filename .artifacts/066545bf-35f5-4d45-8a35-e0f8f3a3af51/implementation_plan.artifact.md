# Implementation Plan - Fix `findNavController` Build Error in `ProfileFragment`

The user is experiencing a build error in `ProfileFragment.kt` because the compiler is attempting to resolve `findNavController(R.id.main)` to the `Fragment` extension function (which takes no arguments) instead of the `Activity` extension function or the `Navigation` utility method.

## User Review Required

> [!IMPORTANT]
> The fix involves switching to `Navigation.findNavController(requireActivity(), R.id.main)` to explicitly target the Activity's navigation host. This is necessary because `ProfileFragment` is nested within a `HomeFragment`'s child navigation host, and using the simple `findNavController()` would return the inner controller instead of the outer one needed for logging out.

## Proposed Changes

### [Component: UI - Profile]

#### [MODIFY] [ProfileFragment.kt](file:///D:/AndroidStudioProjects/PetCare/app/src/main/java/com/rijana/petcare/ui/profile/ProfileFragment.kt)

- Add `import androidx.navigation.Navigation`
- Update `logOut()` function to use `Navigation.findNavController(requireActivity(), R.id.main)` to reach the outer navigation graph.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure the build error is resolved.

### Manual Verification
- N/A (Build fix)
