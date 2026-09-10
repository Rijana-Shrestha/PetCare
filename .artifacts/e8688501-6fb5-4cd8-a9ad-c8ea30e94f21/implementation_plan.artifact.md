# Fix Unresolved Reference 'FragmentAddGroomingAppointmentBinding'

The build error `Unresolved reference 'FragmentAddGroomingAppointmentBinding'` is caused by the missing layout file `fragment_add_grooming_appointment.xml`. View Binding generates binding classes based on layout files, and since this file is missing, the binding class is not generated.

## Proposed Changes

### Layouts

#### [NEW] [fragment_add_grooming_appointment.xml](file:///D:/AndroidStudioProjects/PetCare/app/src/main/res/layout/fragment_add_grooming_appointment.xml)
Create the missing layout file for the grooming appointment screen. I will use `fragment_add_vet_appointment.xml` as a template and update the strings to match grooming context.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference error is resolved.

### Manual Verification
- Once the project builds, the layout can be inspected in the Android Studio layout editor or by running the app and navigating to the Add Grooming Appointment screen.
