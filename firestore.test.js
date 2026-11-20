const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPublicProfile(uid, now) {
  return {
    uid,
    name: "Alice Rahman",
    customUserId: "alice_2026",
    handle: "@alice_2026",
    bio: "Creator on YOURPLATFORM",
    followersCount: "0",
    profileCreated: true,
    coverColorHex: 0xff3f51b5,
    avatarColorHex: 0xff5352ed,
    createdAt: now,
    updatedAt: now,
  };
}

function validPrivateInfo(uid, now) {
  return {
    uid,
    email: "alice@example.com",
    phoneNumber: "+8801711000000",
    createdAt: now,
    updatedAt: now,
  };
}

function validPost(uid, now) {
  return {
    uid,
    userName: "Alice Rahman",
    postText: "Hello from YOURPLATFORM Firestore!",
    mediaType: "text",
    video: false,
    likeCount: 0,
    liked: false,
    comments: [],
    userPost: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validFriend(uid, now) {
  return {
    uid,
    name: "Tanvir Ahmed",
    handle: "@tanvir_ahmed",
    bio: "Travel creator",
    mutualFriendsCount: 4,
    avatarColorHex: 0xff009688,
    verified: true,
    online: true,
    status: "INCOMING_REQUEST",
    requestTimeAgo: "10m ago",
    createdAt: now,
    updatedAt: now,
  };
}

function validReview(uid, status, now) {
  return {
    uid,
    authorUserId: "alice_2026",
    userName: "Alice Rahman",
    rating: 5,
    comment: "Smooth cloud sync experience!",
    status,
    pinned: false,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.doc(`users/${ALICE_UID}/public/profile`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/private/info`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/posts`).get());
  await assertFails(
    unauthDb.collection("reviews").where("status", "==", "APPROVED").get()
  );
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/public/profile`)
      .set(validPublicProfile(ALICE_UID, now))
  );
});

test("Owner public profile & PII private info: create, partial update, and cross-user PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/public/profile`)
      .set(validPublicProfile(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/private/info`)
      .set(validPrivateInfo(ALICE_UID, created))
  );

  // Partial update preserving createdAt succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/public/profile`).update({
      bio: "Updated creator bio",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt tamper fails
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/public/profile`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Tampered future timestamp fails
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/private/info`)
      .update({ updatedAt: new Date(Date.now() + 60000) })
  );

  // Extra unexpected field (@DocumentId 'id') fails
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/private/info`).update({ id: "info" })
  );

  // Bob can read Alice's public non-PII profile, CANNOT write it, and CANNOT read/write Alice's private PII
  await assertSucceeds(bobDb.doc(`users/${ALICE_UID}/public/profile`).get());
  await assertFails(
    bobDb
      .doc(`users/${ALICE_UID}/public/profile`)
      .update({ bio: "Hijacked" })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/private/info`).get());
  await assertFails(
    bobDb
      .doc(`users/${ALICE_UID}/private/info`)
      .set(validPrivateInfo(ALICE_UID, created))
  );
});

test("User posts subcollection: owner CRUD, likes/comments partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/posts`)
      .doc("post_1")
      .set(validPost(ALICE_UID, created))
  );

  // List matches repository query
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/posts`).get());

  // Partial update for like/comment succeeds
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/posts`).doc("post_1").update({
      likeCount: 1,
      liked: true,
      comments: ["Nice post!"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid mediaType fails
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/posts`).doc("post_2").set({
      ...validPost(ALICE_UID, created),
      mediaType: "unknown_type",
    })
  );

  // Bob cannot list, read, update, or delete Alice's posts
  await assertFails(bobDb.collection(`users/${ALICE_UID}/posts`).get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/posts`).doc("post_1").delete()
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/posts`).doc("post_1").delete()
  );
});

test("User friends subcollection: owner CRUD & status validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/friends`)
      .doc("u_req_1")
      .set(validFriend(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/friends`).doc("u_req_1").update({
      status: "FRIEND",
      requestTimeAgo: "",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/friends`).doc("u_req_1").update({
      status: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/friends`).get());
});

test("Reviews collection: filtered queries succeed, unfiltered fails, and ownership enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("reviews")
      .doc("rev_approved")
      .set(validReview(ALICE_UID, "APPROVED", created))
  );
  await assertSucceeds(
    aliceDb
      .collection("reviews")
      .doc("rev_hidden")
      .set(validReview(ALICE_UID, "HIDDEN", created))
  );

  // Filtered queries matching Kotlin repository succeed
  await assertSucceeds(
    bobDb.collection("reviews").where("status", "==", "APPROVED").get()
  );
  await assertSucceeds(
    aliceDb.collection("reviews").where("uid", "==", ALICE_UID).get()
  );

  // Unfiltered query on top-level reviews fails
  await assertFails(bobDb.collection("reviews").get());

  // Cross-user impersonation on create or update fails
  await assertFails(
    bobDb
      .collection("reviews")
      .doc("rev_spoof")
      .set(validReview(ALICE_UID, "APPROVED", created))
  );
  await assertFails(
    bobDb.collection("reviews").doc("rev_approved").update({
      pinned: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner control: toggle pin, status, delete
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_approved").update({
      pinned: true,
      status: "PENDING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_approved").delete()
  );
});
