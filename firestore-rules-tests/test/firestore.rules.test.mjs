import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";

import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const PROJECT_ID = "demo-innovfix";
const ORG_ID = "innovfix";
const CONVERSATION_ID = "direct-alice-bob";
const BASE_MESSAGE_ID = "01912345-6789-7abc-8def-0123456789ab";
const SECOND_MESSAGE_ID = "01912345-6789-7abd-8def-0123456789ab";
const MISSING_MESSAGE_ID = "01912345-6789-7abf-8def-0123456789ab";
const EPOCH = Timestamp.fromMillis(0);
const SEED_TIME = Timestamp.fromMillis(1_700_000_000_000);

const here = dirname(fileURLToPath(import.meta.url));
const rulesPath = resolve(here, "../../firestore.rules");
const storageRulesPath = resolve(here, "../../storage.rules");
const emulatorAddress = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";
const separator = emulatorAddress.lastIndexOf(":");
const emulatorHost = emulatorAddress.slice(0, separator);
const emulatorPort = Number(emulatorAddress.slice(separator + 1));

let testEnvironment;

function profile({ active = true, name = "Employee" } = {}) {
  return {
    email: `${name.toLowerCase()}@innovfix.test`,
    displayName: name,
    role: "MEMBER",
    active,
    avatarKey: null,
    notificationPreference: "ALL",
    lastSeenAt: null,
    activeUntil: null,
    createdAt: SEED_TIME,
    updatedAt: SEED_TIME,
  };
}

function conversationData() {
  return {
    orgId: ORG_ID,
    type: "DIRECT",
    memberIds: ["alice", "bob"],
    title: null,
    createdBy: "alice",
    createdAt: SEED_TIME,
    updatedAt: SEED_TIME,
    schemaVersion: 1,
  };
}

function conversationMember(uid) {
  return {
    userId: uid,
    role: "MEMBER",
    joinedAt: SEED_TIME,
    lastDeliveredAt: EPOCH,
    lastReadAt: EPOCH,
    updatedAt: SEED_TIME,
  };
}

function storedMessage(senderId = "alice", overrides = {}) {
  return {
    senderId,
    text: "Seeded message",
    clientCreatedAt: SEED_TIME,
    serverCreatedAt: SEED_TIME,
    replyToMessageId: null,
    deletedAt: null,
    schemaVersion: 1,
    ...overrides,
  };
}

function newMessage(senderId = "alice", overrides = {}) {
  return {
    senderId,
    text: "A new message",
    clientCreatedAt: Timestamp.now(),
    serverCreatedAt: serverTimestamp(),
    replyToMessageId: null,
    deletedAt: null,
    schemaVersion: 1,
    kind: "TEXT",
    storagePath: null,
    fileName: null,
    mimeType: null,
    sizeBytes: null,
    durationMillis: null,
    ...overrides,
  };
}

function inboxData() {
  return {
    conversationId: CONVERSATION_ID,
    orgId: ORG_ID,
    conversationType: "DIRECT",
    title: "Bob",
    lastMessageId: null,
    lastMessagePreview: "",
    lastMessageSenderId: null,
    lastMessageAt: null,
    unreadCount: 0,
    muted: false,
    pinned: false,
    updatedAt: SEED_TIME,
  };
}

function employeeDb(uid, orgId = ORG_ID) {
  return testEnvironment.authenticatedContext(uid, { orgId }).firestore();
}

function messageRef(database, messageId = BASE_MESSAGE_ID) {
  return doc(database, "conversations", CONVERSATION_ID, "messages", messageId);
}

async function seedFixture() {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await Promise.all([
      setDoc(doc(database, "organizations", ORG_ID, "members", "alice"), profile({ name: "Alice" })),
      setDoc(doc(database, "organizations", ORG_ID, "members", "bob"), profile({ name: "Bob" })),
      setDoc(doc(database, "organizations", ORG_ID, "members", "mallory"), profile({ name: "Mallory" })),
      setDoc(
        doc(database, "organizations", ORG_ID, "members", "disabled"),
        profile({ active: false, name: "Disabled" }),
      ),
      setDoc(
        doc(database, "organizations", "other-org", "members", "outsider"),
        profile({ name: "Outsider" }),
      ),
      setDoc(doc(database, "conversations", CONVERSATION_ID), conversationData()),
      setDoc(
        doc(database, "conversations", CONVERSATION_ID, "members", "alice"),
        conversationMember("alice"),
      ),
      setDoc(
        doc(database, "conversations", CONVERSATION_ID, "members", "bob"),
        conversationMember("bob"),
      ),
      setDoc(messageRef(database), storedMessage()),
      setDoc(
        doc(database, "members", "alice", "inbox", CONVERSATION_ID),
        inboxData(),
      ),
      setDoc(
        doc(database, "members", "bob", "inbox", CONVERSATION_ID),
        { ...inboxData(), title: "Alice" },
      ),
    ]);
  });
}

before(async () => {
  const rules = await readFile(rulesPath, "utf8");
  const storageRules = await readFile(storageRulesPath, "utf8");
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: emulatorHost,
      port: emulatorPort,
      rules,
    },
    storage: {
      rules: storageRules,
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await testEnvironment.clearStorage();
  await seedFixture();
});

after(async () => {
  await testEnvironment?.cleanup();
});

test("active organization members can read authorized directory and conversation data", async () => {
  const database = employeeDb("alice");
  const directoryQuery = query(
    collection(database, "organizations", ORG_ID, "members"),
    where("active", "==", true),
    orderBy("displayName"),
  );
  const directory = await assertSucceeds(getDocs(directoryQuery));
  assert.equal(directory.docs.length, 3);

  await assertSucceeds(getDoc(doc(database, "conversations", CONVERSATION_ID)));
  const historyQuery = query(
    collection(database, "conversations", CONVERSATION_ID, "messages"),
    orderBy("serverCreatedAt", "desc"),
    limit(50),
  );
  const history = await assertSucceeds(getDocs(historyQuery));
  assert.equal(history.docs.length, 1);
});

test("conversation media is private to active members and immutable", async () => {
  const path = `organizations/${ORG_ID}/conversations/${CONVERSATION_ID}/messages/${BASE_MESSAGE_ID}/voice.m4a`;
  const aliceStorage = testEnvironment.authenticatedContext("alice", { orgId: ORG_ID })
    .storage("gs://demo-innovfix.appspot.com");
  const bobStorage = testEnvironment.authenticatedContext("bob", { orgId: ORG_ID })
    .storage("gs://demo-innovfix.appspot.com");
  const nonMemberStorage = testEnvironment.authenticatedContext("mallory", { orgId: ORG_ID })
    .storage("gs://demo-innovfix.appspot.com");
  const anonymousStorage = testEnvironment.unauthenticatedContext()
    .storage("gs://demo-innovfix.appspot.com");

  await assertSucceeds(aliceStorage.ref(path).put(new Uint8Array([1, 2, 3]), { contentType: "audio/mp4" }));
  await assertSucceeds(bobStorage.ref(path).getDownloadURL());
  await assertFails(nonMemberStorage.ref(path).getDownloadURL());
  await assertFails(anonymousStorage.ref(path).getDownloadURL());
  await assertFails(aliceStorage.ref(path).put(new Uint8Array([4]), { contentType: "audio/mp4" }));
  await assertFails(aliceStorage.ref(path).delete());
});

test("conversation media rejects unrelated paths and files over 25 MiB", async () => {
  const aliceStorage = testEnvironment.authenticatedContext("alice", { orgId: ORG_ID })
    .storage("gs://demo-innovfix.appspot.com");
  const validPath = `organizations/${ORG_ID}/conversations/${CONVERSATION_ID}/messages/${SECOND_MESSAGE_ID}/large.bin`;
  const unrelatedPath = `organizations/${ORG_ID}/avatars/alice/photo.jpg`;

  await assertFails(aliceStorage.ref(unrelatedPath).put(new Uint8Array([1]), { contentType: "image/jpeg" }));
  await assertFails(
    aliceStorage.ref(validPath).put(new Uint8Array(25 * 1024 * 1024 + 1), {
      contentType: "application/octet-stream",
    }),
  );
});

test("unauthenticated, disabled, cross-organization, and non-member access is denied", async () => {
  const anonymous = testEnvironment.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anonymous, "conversations", CONVERSATION_ID)));

  await assertFails(
    getDoc(doc(employeeDb("disabled"), "conversations", CONVERSATION_ID)),
  );
  await assertFails(
    getDoc(doc(employeeDb("outsider", "other-org"), "conversations", CONVERSATION_ID)),
  );
  await assertFails(
    getDoc(doc(employeeDb("alice", "other-org"), "conversations", CONVERSATION_ID)),
  );
  await assertFails(
    getDoc(doc(employeeDb("mallory"), "conversations", CONVERSATION_ID)),
  );
  await assertFails(getDoc(messageRef(employeeDb("mallory"))));
});

test("a member can create an idempotent UUIDv7 message as themselves", async () => {
  const database = employeeDb("alice");
  await assertSucceeds(
    setDoc(messageRef(database, SECOND_MESSAGE_ID), newMessage()),
  );

  const stored = await assertSucceeds(getDoc(messageRef(database, SECOND_MESSAGE_ID)));
  assert.equal(stored.data().senderId, "alice");
  assert.equal(stored.data().text, "A new message");
});

test("message validation rejects forged senders, oversized text, unknown keys, bad IDs, and invalid replies", async () => {
  const alice = employeeDb("alice");
  await assertFails(
    setDoc(messageRef(alice, SECOND_MESSAGE_ID), newMessage("bob")),
  );
  await assertFails(
    setDoc(
      messageRef(alice, SECOND_MESSAGE_ID),
      newMessage("alice", { text: "x".repeat(4_001) }),
    ),
  );
  await assertFails(
    setDoc(
      messageRef(alice, SECOND_MESSAGE_ID),
      { ...newMessage(), admin: true },
    ),
  );
  await assertFails(
    setDoc(messageRef(alice, "not-a-uuid"), newMessage()),
  );
  await assertFails(
    setDoc(
      messageRef(alice, SECOND_MESSAGE_ID),
      newMessage("alice", { replyToMessageId: MISSING_MESSAGE_ID }),
    ),
  );
  await assertFails(
    setDoc(messageRef(employeeDb("mallory"), SECOND_MESSAGE_ID), newMessage("mallory")),
  );

  await assertSucceeds(
    setDoc(
      messageRef(alice, SECOND_MESSAGE_ID),
      newMessage("alice", { replyToMessageId: BASE_MESSAGE_ID }),
    ),
  );
});

test("only the sender can soft-delete a message and nobody can hard-delete it", async () => {
  const aliceRef = messageRef(employeeDb("alice"));
  const bobRef = messageRef(employeeDb("bob"));

  await assertFails(
    updateDoc(bobRef, { text: "", deletedAt: serverTimestamp() }),
  );
  await assertFails(updateDoc(aliceRef, { text: "edited" }));
  await assertSucceeds(
    updateDoc(aliceRef, { text: "", deletedAt: serverTimestamp() }),
  );
  await assertFails(deleteDoc(aliceRef));
});

test("members can manage only their own supported reaction", async () => {
  const alice = employeeDb("alice");
  const reactionId = `${BASE_MESSAGE_ID}_alice`;
  const reactionRef = doc(
    alice,
    "conversations",
    CONVERSATION_ID,
    "reactions",
    reactionId,
  );
  await assertSucceeds(
    setDoc(reactionRef, {
      messageId: BASE_MESSAGE_ID,
      userId: "alice",
      emoji: "👍",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    updateDoc(reactionRef, { emoji: "😂", updatedAt: serverTimestamp() }),
  );
  await assertFails(
    updateDoc(reactionRef, { emoji: "🔥", updatedAt: serverTimestamp() }),
  );

  const forgedRef = doc(
    employeeDb("bob"),
    "conversations",
    CONVERSATION_ID,
    "reactions",
    `${BASE_MESSAGE_ID}_alice`,
  );
  await assertFails(
    setDoc(forgedRef, {
      messageId: BASE_MESSAGE_ID,
      userId: "alice",
      emoji: "❤️",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(deleteDoc(reactionRef));
});

test("typing state is short-lived and writable only by its owner", async () => {
  const future = Timestamp.fromMillis(Date.now() + 10_000);
  const aliceTyping = doc(
    employeeDb("alice"),
    "conversations",
    CONVERSATION_ID,
    "typing",
    "alice",
  );
  await assertSucceeds(
    setDoc(aliceTyping, {
      userId: "alice",
      isTyping: true,
      expiresAt: future,
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(
      doc(
        employeeDb("bob"),
        "conversations",
        CONVERSATION_ID,
        "typing",
        "alice",
      ),
      {
        userId: "alice",
        isTyping: true,
        expiresAt: future,
        updatedAt: serverTimestamp(),
      },
    ),
  );
  await assertFails(
    setDoc(aliceTyping, {
      userId: "alice",
      isTyping: true,
      expiresAt: Timestamp.fromMillis(Date.now() + 60_000),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    setDoc(aliceTyping, {
      userId: "alice",
      isTyping: false,
      expiresAt: Timestamp.fromMillis(Date.now() - 1_000),
      updatedAt: serverTimestamp(),
    }),
  );
});

test("delivery and read watermarks are owner-only, monotonic, and ordered", async () => {
  const aliceMember = doc(
    employeeDb("alice"),
    "conversations",
    CONVERSATION_ID,
    "members",
    "alice",
  );
  const delivered = Timestamp.fromMillis(Date.now() - 2_000);
  const read = Timestamp.fromMillis(Date.now() - 3_000);
  await assertSucceeds(
    updateDoc(aliceMember, {
      lastDeliveredAt: delivered,
      lastReadAt: read,
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(aliceMember, {
      lastDeliveredAt: Timestamp.fromMillis(Date.now() - 4_000),
      lastReadAt: Timestamp.fromMillis(Date.now() - 4_000),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(aliceMember, {
      lastDeliveredAt: Timestamp.fromMillis(Date.now() - 1_000),
      lastReadAt: Timestamp.now(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(
      doc(
        employeeDb("bob"),
        "conversations",
        CONVERSATION_ID,
        "members",
        "alice",
      ),
      {
        lastDeliveredAt: Timestamp.now(),
        updatedAt: serverTimestamp(),
      },
    ),
  );
});

test("ordinary clients cannot create conversations or change membership and roles", async () => {
  const alice = employeeDb("alice");
  await assertFails(
    setDoc(doc(alice, "conversations", "client-created"), conversationData()),
  );
  await assertFails(
    updateDoc(doc(alice, "conversations", CONVERSATION_ID), {
      memberIds: ["alice", "bob", "mallory"],
    }),
  );
  await assertFails(
    updateDoc(
      doc(alice, "conversations", CONVERSATION_ID, "members", "alice"),
      { role: "ADMIN", updatedAt: serverTimestamp() },
    ),
  );
  await assertFails(
    setDoc(
      doc(alice, "conversations", CONVERSATION_ID, "members", "mallory"),
      conversationMember("mallory"),
    ),
  );
  await assertFails(
    updateDoc(doc(alice, "organizations", ORG_ID, "members", "alice"), {
      role: "ADMIN",
      updatedAt: serverTimestamp(),
    }),
  );
});

test("employees can update only their own notification and presence settings", async () => {
  const aliceProfile = doc(
    employeeDb("alice"),
    "organizations",
    ORG_ID,
    "members",
    "alice",
  );
  await assertSucceeds(
    updateDoc(aliceProfile, {
      notificationPreference: "NONE",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    updateDoc(aliceProfile, {
      lastSeenAt: serverTimestamp(),
      activeUntil: Timestamp.fromMillis(Date.now() + 60_000),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(
      doc(
        employeeDb("bob"),
        "organizations",
        ORG_ID,
        "members",
        "alice",
      ),
      { notificationPreference: "ALL", updatedAt: serverTimestamp() },
    ),
  );
});

test("inbox reads and settings are owner-only while server-maintained fields are protected", async () => {
  const aliceInbox = doc(
    employeeDb("alice"),
    "members",
    "alice",
    "inbox",
    CONVERSATION_ID,
  );
  await assertSucceeds(getDoc(aliceInbox));
  await assertSucceeds(
    updateDoc(aliceInbox, {
      muted: true,
      pinned: true,
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(aliceInbox, { unreadCount: 1, updatedAt: serverTimestamp() }),
  );
  await assertFails(
    getDoc(
      doc(
        employeeDb("bob"),
        "members",
        "alice",
        "inbox",
        CONVERSATION_ID,
      ),
    ),
  );
  await assertFails(
    setDoc(
      doc(employeeDb("alice"), "members", "alice", "inbox", "forged"),
      { ...inboxData(), conversationId: "forged" },
    ),
  );
});

test("FCM device registrations are owner-only and use a fixed schema", async () => {
  const alice = employeeDb("alice");
  const deviceRef = doc(alice, "members", "alice", "devices", "install-1");
  await assertSucceeds(
    setDoc(deviceRef, {
      installationId: "install-1",
      token: "fcm-token-1",
      platform: "ANDROID",
      enabled: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    updateDoc(deviceRef, { token: "fcm-token-2", updatedAt: serverTimestamp() }),
  );
  await assertFails(
    setDoc(
      doc(employeeDb("bob"), "members", "alice", "devices", "install-2"),
      {
        installationId: "install-2",
        token: "stolen-token",
        platform: "ANDROID",
        enabled: true,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      },
    ),
  );
  await assertFails(
    updateDoc(deviceRef, { platform: "IOS", updatedAt: serverTimestamp() }),
  );
  await assertSucceeds(deleteDoc(deviceRef));
});
