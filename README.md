# Mental Health Support App

This Android application is a mental‑health support platform that helps users understand and manage their wellbeing. It combines evidence‑based self‑assessment tools, AI‑powered recommendations, and communication with mental‑health professionals.

## Key Features

- **Role selection**
  - Choose between *User* and *Doctor* roles from the launcher `RoleSelectActivity`.

- **Authentication**
  - User login and signup.
  - Doctor login and signup.
  - Uses Firebase Authentication for managing accounts.

- **Self‑Assessment (Wellbeing Check‑in)**
  - `SelfAssessmentActivity` with tabs (ViewPager2 + TabLayout):
    - Depression: **PHQ‑9** questionnaire.
    - Anxiety: **GAD‑7** questionnaire.
    - Stress: **PSS‑10** questionnaire.
  - Calculates severity levels (Minimal, Mild, Moderate, etc.).
  - Saves assessment results to **Cloud Firestore** under each user.
  - Computes an overall **wellness score** and stores progress snapshots for charts.

- **AI‑Powered Recommendations**
  - `AIRecommendationsActivity`, `RecommendationsActivity`, and `RecommendationDetailActivity`.
  - Generates personalized coping strategies and activities based on assessment data and user context.

- **Progress Tracking**
  - `ProgressActivity` with visualizations (e.g., bar charts) using `SimpleBarChartView`.
  - Shows trends over time for wellness scores and assessments.

- **Professional Help & Appointments**
  - `ProfessionalHelpActivity` and `DoctorListActivity` to browse doctors.
  - `BookAppointmentActivity` to schedule sessions.
  - `UpcomingAppointmentsActivity` and `AppointmentHistoryActivity` for tracking upcoming and past sessions.
  - Separate flows for doctors: `DoctorAppointmentsActivity`, `DoctorHistoryActivity`, `DoctorProfileActivity`, and doctor notes.

- **Real‑time Communication**
  - `ChatActivity` with `ChatMessageAdapter` for text chat.
  - `AudioCallActivity` and `VideoCallActivity` for calling.
  - Uses Firebase Cloud Messaging via `MyFirebaseService` for notifications.

- **Reminders & Notifications**
  - `ReminderReceiver` and POST_NOTIFICATIONS permission.
  - Appointment reminders and assessment nudges.

## Tech Stack

- **Platform:** Android (Java)
- **Minimum SDK / Target SDK:** Configure in `app/build.gradle` (Android Gradle Plugin)
- **Architecture:** Activity + Fragment based navigation
- **Backend:**
  - Firebase Authentication
  - Firebase Cloud Firestore
  - Firebase Cloud Messaging
- **UI Components:**
  - AppCompat, Material Components (TabLayout, ViewPager2, etc.)

## Project Structure (High Level)

- `app/src/main/java/com/example/project/`
  - `MainActivity`, `RoleSelectActivity`, `HomeActivity`
  - `SelfAssessmentActivity`, `DepressionFragment`, `AnxietyFragment`, `StressFragment`
  - `AIRecommendationsActivity`, `RecommendationsActivity`, `RecommendationDetailActivity`
  - `ProgressActivity`, `SimpleBarChartView`
  - Appointment‑related activities and adapters
  - Doctor‑related activities and adapters
  - `ChatActivity`, `AudioCallActivity`, `VideoCallActivity`
  - Firebase service classes and model classes (e.g., `AssessmentData`, `UserAssessment`, `Recommendation`)
- `app/src/main/res/`
  - Layout XMLs for each activity/fragment
  - Drawable assets, colors, themes, and strings
- `AndroidManifest.xml`
  - Declares all activities, Firebase messaging service, and required permissions.

## Getting Started

### Prerequisites

- Android Studio (latest stable version)
- Java 8+ support in Android Studio
- A Firebase project configured for:
  - Authentication
  - Cloud Firestore
  - Cloud Messaging (FCM)

### Setup Steps

1. **Clone or open the project**
   - Open this folder in Android Studio.

2. **Configure Firebase**
   - Create a Firebase project in the Firebase Console.
   - Add an Android app with your package name (e.g., `com.example.project`).
   - Download `google-services.json` and place it in `app/`.
   - Enable **Authentication** (Email/Password or chosen providers).
   - Create Firestore database (in *Production* or *Test* mode).

3. **Check Gradle configuration**
   - Open `build.gradle.kts` and `app/build.gradle` (or `build.gradle.kts` for app module).
   - Ensure Firebase dependencies and Google services plugin are applied.
   - Sync Gradle.

4. **Run the app**
   - Select a device/emulator.
   - Build and run.
   - The launcher will open `RoleSelectActivity` where you choose *User* or *Doctor*.

## Usage Overview

1. **User Flow**
   - Select **User** role.
   - Sign up or log in.
   - From `HomeActivity`, access:
     - Self‑assessment (PHQ‑9, GAD‑7, PSS‑10).
     - AI recommendations.
     - Progress charts.
     - Professional help and appointment booking.

2. **Doctor Flow**
   - Select **Doctor** role.
   - Log in or sign up.
   - View assigned appointments, patient history, and add notes.

3. **Self‑Assessment & Progress**
   - Complete one or more questionnaires in `SelfAssessmentActivity`.
   - Submit to save scores and severity levels to Firestore.
   - View results in `AssessmentResultsActivity` and track progress over time in `ProgressActivity`.

## Notes & Customization

- Update strings, colors, and themes in `res/values/` to match your branding.
- Adjust severity thresholds or wellness score logic in `SelfAssessmentActivity` if your research or requirements differ.
- Extend recommendation logic or integrate external AI APIs in `RecommendationService`.

## Disclaimer

This app is intended for **support and education only** and does **not** replace professional medical advice, diagnosis, or treatment. Always consult qualified health professionals for any mental‑health concerns.
