# Walkthrough - Fixing Unresolved Reference 'tvPetDetails'

I have fixed the build error `Unresolved reference 'tvPetDetails'` in `HomePetAdapter.kt` and improved string handling in the adapters.

## Changes Made

### UI Adapters

#### [MODIFY] [HomePetAdapter.kt](file:///D:/AndroidStudioProjects/PetCare/app/src/main/java/com/rijana/petcare/ui/home/HomePetAdapter.kt)
- Fixed the unresolved reference by updating `bind()` to use `tvPetAge` and `tvPetWeight` which are the actual IDs present in `item_pet_home.xml`.
- Refactored `calculateAge()` to accept `Context` and use resource strings instead of hardcoded strings.
- Used `context.getString()` for weight formatting to resolve lint warnings.

#### [MODIFY] [PetAdapter.kt](file:///D:/AndroidStudioProjects/PetCare/app/src/main/java/com/rijana/petcare/ui/pets/PetAdapter.kt)
- Similar refactoring as `HomePetAdapter.kt` to use resource strings for age and weight, ensuring consistency across the app and resolving lint warnings.

### Resources

#### [MODIFY] [strings.xml](file:///D:/AndroidStudioProjects/PetCare/app/src/main/res/values/strings.xml)
- Added new resource strings for pet details:
    - `pet_weight_format`: For localized weight display.
    - `pet_age_less_than_year`: For pets younger than a year.
    - `pet_age_years`: For pet age in years.
    - `pet_details_format`: For combined age and weight display in the Pets list.

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin` and the build finished successfully.

> [!NOTE]
> The issue was caused by a mismatch between the view IDs in the layout file `item_pet_home.xml` and the code in `HomePetAdapter.kt`. The adapter was trying to access `tvPetDetails` which didn't exist in that specific layout.
