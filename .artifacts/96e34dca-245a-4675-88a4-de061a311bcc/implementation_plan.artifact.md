# Implementation Plan - Fix Unresolved reference 'FragmentSavedPlacesBinding'

The build error `Unresolved reference 'FragmentSavedPlacesBinding'` in `SavedPlacesListFragment.kt` is caused by a mismatch between the layout file name and the expected ViewBinding class name. The layout is currently named `fragment_saved_place.xml` (singular), which generates `FragmentSavedPlaceBinding`, but the Kotlin code expects `FragmentSavedPlacesBinding` (plural).

## Proposed Changes

### [Component: UI - Map]

#### [MODIFY] Rename Layout Resource
- Rename `app/src/main/res/layout/fragment_saved_place.xml` to `app/src/main/res/layout/fragment_saved_places.xml`.
- Since there are no direct references to `R.layout.fragment_saved_place` in the code (it's accessed via ViewBinding), this change will only affect the generated Binding class name.

#### [VERIFY] [SavedPlacesListFragment.kt](file:///D:/AndroidStudioProjects/PetCare/app/src/main/java/com/rijana/petcare/ui/map/SavedPlacesListFragment.kt)
- After renaming the layout, the generated class `FragmentSavedPlacesBinding` should become available.
- Verify that the import and usage in `SavedPlacesListFragment.kt` are correct (they already seem to be plural).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure the project builds successfully.

### Manual Verification
- None required as this is a build fix.
