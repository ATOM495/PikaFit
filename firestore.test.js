const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CREATOR_UID = "creator_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSessionPayload(uid, timestamp) {
  return {
    userId: uid,
    level: 2,
    rank: "E-Rank Hunter",
    currentXp: 620,
    nextLevelXp: 1000,
    progressPercent: 62.0,
    hpPercent: 100.0,
    streakDays: 4,
    caloriesLogged: 2000,
    caloriesTarget: 2588,
    proteinG: 112.0,
    carbsG: 310.0,
    fatsG: 48.0,
    fiberG: 22.0,
    omega3Mg: 1900.0,
    waterLiters: 4.0,
    targetLiters: 4.0,
    workoutCompleted: true,
    tacticalBriefing: "2000 kcal intake logged. Vitality replenished.",
    stateJson: "{\"active_buffs\":[\"Well-Fed (+50 XP)\"]}",
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

function validJournalPayload(uid, timestamp) {
  return {
    userId: uid,
    entryType: "PHOTO_PROGRESS",
    title: "Week 1 Physique Check-In",
    caption: "Upper body hypertrophy check-in at 72.5 kg.",
    poseTag: "Front Double Bicep",
    photoBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    weightKg: 72.5,
    heightCm: 178.0,
    age: 24,
    bmi: 22.9,
    bodyFatPercent: 14.2,
    muscleMassPercent: 44.8,
    createdAt: timestamp,
  };
}

function validFeedbackPayload(uid, timestamp) {
  return {
    userId: uid,
    hunterName: "Shadow Vanguard",
    category: "FEATURE_REQUEST",
    rating: 5,
    message: "Love the Recharts progression graphs! Please add custom rest timer alerts.",
    status: "OPEN",
    visibility: "PUBLIC",
    createdAt: timestamp,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sessions").doc("active").get());
});

test("Authenticated user: can create and read their own valid session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("active").set(validSessionPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("active").get()
  );
});

test("Authenticated user: cannot read another user's session", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("sessions").doc("active").set(validSessionPayload(BOB_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("sessions").doc("active").get()
  );
});

test("Shadow Create Test: rejects session creation with ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const payload = { ...validSessionPayload(ALICE_UID, now), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("active").set(payload)
  );
});

test("Value Poisoning Test: rejects update with invalid type or immutable createdAt change", async () => {
  const now = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("sessions").doc("active").set(validSessionPayload(ALICE_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("active").update({
      caloriesLogged: "two thousand",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("TacticalLog: owner can create and list logs, but cannot update logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("logs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      commandText: "i had 2000 kcal",
      briefingResponse: "2000 kcal logged.",
      xpDelta: 80,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("logs").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    logRef.update({ xpDelta: 9999 })
  );
});

test("JournalEntry: owner can create, list, and delete journal entries, but cannot update or cross-read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const entryRef = aliceDb.collection("users").doc(ALICE_UID).collection("journal").doc("entry_1");

  await assertSucceeds(entryRef.set(validJournalPayload(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("journal").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("journal").doc("entry_1").get()
  );
  await assertFails(
    entryRef.update({ weightKg: 99.0 })
  );
  await assertSucceeds(entryRef.delete());
});

test("FeedbackEntry: public user can submit feedback and query public feed, while unverified email spoof fails", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofCreatorDb = testEnv.authenticatedContext(BOB_UID, {
    email: "tmcrainbow093@gmail.com",
    email_verified: false,
  }).firestore();
  const verifiedCreatorDb = testEnv.authenticatedContext(CREATOR_UID, {
    email: "tmcrainbow093@gmail.com",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 1000);
  const fbRef = aliceDb.collection("feedback").doc("fb_1");

  // Unauthenticated cannot read or write feedback
  await assertFails(unauthDb.collection("feedback").doc("fb_1").get());

  // Alice submits public feedback
  await assertSucceeds(fbRef.set(validFeedbackPayload(ALICE_UID, now)));

  // Public query with visibility == PUBLIC succeeds for signed-in users
  await assertSucceeds(
    bobDb.collection("feedback").where("visibility", "==", "PUBLIC").get()
  );

  // Bob (non-owner, non-creator) cannot update status or delete Alice's feedback
  await assertFails(
    bobDb.collection("feedback").doc("fb_1").update({ status: "IMPLEMENTED" })
  );
  await assertFails(
    bobDb.collection("feedback").doc("fb_1").delete()
  );

  // Email spoofing test: unverified creator email MUST be rejected
  await assertFails(
    spoofCreatorDb.collection("feedback").doc("fb_1").update({ status: "IMPLEMENTED" })
  );

  // Verified app creator can triage and update feedback status to IMPLEMENTED
  await assertSucceeds(
    verifiedCreatorDb.collection("feedback").doc("fb_1").update({ status: "IMPLEMENTED" })
  );
});
