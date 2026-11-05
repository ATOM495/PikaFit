# Security Specification: Hunter System

## 1. Data Invariants
1. **Authentication & Ownership Invariant**: Every read (`get`, `list`) and write (`create`, `update`, `delete`) on `/users/{userId}/sessions/{sessionId}`, `/users/{userId}/logs/{logId}`, and `/users/{userId}/journal/{entryId}` requires `request.auth != null`, `request.auth.uid == userId`, and `incoming().userId == request.auth.uid`.
2. **Public Feedback & Creator Inbox Invariant (`/feedback/{feedbackId}`)**:
   - Any signed-in user can create a `FeedbackEntry` only with `incoming().userId == request.auth.uid`, `incoming().status == 'OPEN'`, and `incoming().visibility == 'PUBLIC'`.
   - No PII (`email`, `phone`, `address`) is stored in `FeedbackEntry`.
   - `list` queries on `/feedback/{feedbackId}` require `existing().visibility == 'PUBLIC'` or `existing().userId == request.auth.uid` or verified app creator (`isAppCreator()`).
   - Only the feedback author or the verified app creator (`request.auth.token.email == 'tmcrainbow093@gmail.com' && request.auth.token.email_verified == true`) can update `status` (`OPEN`, `IN_REVIEW`, `IMPLEMENTED`) or delete a feedback entry; all other fields (`userId`, `hunterName`, `category`, `rating`, `message`, `visibility`, `createdAt`) are strictly immutable.
3. **Path Variable Hardening**: `userId`, `sessionId`, `logId`, `entryId`, and `feedbackId` must be valid alphanumeric/underscore/hyphen strings up to 128 characters (`isValidId`).
4. **Strict Key Allowlisting & Anti-Update-Gap**: `create` enforces `hasAll` and `hasOnly` for all schema fields; `update` wraps `isValid[Entity](incoming())`, enforces immutable `userId` and `createdAt`, and restricts `affectedKeys().hasOnly(...)`.
5. **Temporal Integrity**: `createdAt` and `updatedAt` must be Firestore `timestamp` objects `<= request.time`.
6. **Biometrics & Progress Photo Payload Bounds**: `JournalEntry.photoBase64` is bounded to `<= 250000` characters to prevent resource exhaustion ("Denial of Wallet"), while biometric fields (`weightKg`, `heightCm`, `age`, `bmi`, `bodyFatPercent`, `muscleMassPercent`) enforce strict numerical bounds.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/sessions").get()` or `unauthDb.collection("feedback").get()` -> Reject
2. **Cross-User Read**: `bobDb.doc("users/alice/sessions/current").get()` -> Reject
3. **UID Spoofing on Create**: `aliceDb.doc("users/alice/sessions/current").set({ userId: "bob", ... })` -> Reject
4. **Ghost Field Injection (Shadow Create/Update)**: Payload includes `isAdmin: true` -> Reject via `hasOnly`
5. **String Overflow (Resource Poisoning)**: `rank` with 500 chars or `stateJson` > 25000 chars or `photoBase64` > 250000 chars -> Reject
6. **Negative Calorie / XP / Weight / Rating Injection**: `level: 0`, `caloriesLogged: -500`, `weightKg: -10`, or `rating: 0` -> Reject
7. **Timestamp Forgery (Epoch Long instead of ServerTimestamp)**: `createdAt: 1700000000000` -> Reject
8. **Future Timestamp Spoofing**: `createdAt` in the future (`> request.time`) -> Reject
9. **Immutable Field Mutation on Update**: Updating `createdAt`, `userId`, or `message` on existing session/feedback -> Reject
10. **Value Poisoning on Update**: Updating `caloriesLogged` to `"two thousand"` or `status` to `"HACKED"` -> Reject
11. **Email Spoofing on Creator Feedback Update**: User with `email: 'tmcrainbow093@gmail.com'` but `email_verified: false` attempting to update another user's feedback status -> Reject
12. **ID Poisoning**: Document ID containing invalid characters or > 128 chars -> Reject
