import { createHash } from "node:crypto";
import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import {
  Timestamp,
  getFirestore,
  type DocumentData,
  type Firestore,
} from "firebase-admin/firestore";

const ORG_ID = "innovfix";
const PROJECT_ID =
  process.env.GOOGLE_CLOUD_PROJECT ??
  process.env.GCLOUD_PROJECT ??
  "demo-innovfix-internal";
const PASSWORD = process.env.EMULATOR_SEED_PASSWORD ?? "LocalDemoOnly!2026";
const EPOCH = Timestamp.fromMillis(0);

interface SeedEmployee {
  uid: string;
  email: string;
  displayName: string;
  role: "ADMIN" | "MEMBER";
  avatarKey: string;
}

const EMPLOYEES: SeedEmployee[] = [
  {
    uid: "seed-own",
    email: "northstar@innovfix.test",
    displayName: "Northstar Studio",
    role: "ADMIN",
    avatarKey: "avatar_own",
  },
  {
    uid: "seed-mia",
    email: "mia@innovfix.test",
    displayName: "Mia Kapoor",
    role: "MEMBER",
    avatarKey: "avatar_mia",
  },
  {
    uid: "seed-dev",
    email: "dev@innovfix.test",
    displayName: "Dev Product Lead",
    role: "MEMBER",
    avatarKey: "avatar_dev",
  },
  {
    uid: "seed-sara",
    email: "sara@innovfix.test",
    displayName: "Sara Operations",
    role: "MEMBER",
    avatarKey: "avatar_sara",
  },
  {
    uid: "seed-reena",
    email: "reena@innovfix.test",
    displayName: "Reena Creative",
    role: "MEMBER",
    avatarKey: "avatar_reena",
  },
];

function directConversationId(firstUid: string, secondUid: string): string {
  const pair = [firstUid, secondUid].sort();
  const digest = createHash("sha256")
    .update(ORG_ID + ":" + pair[0] + ":" + pair[1], "utf8")
    .digest("hex")
    .slice(0, 40);
  return "direct_" + digest;
}

function assertEmulatorOnly(): void {
  if (!process.env.FIREBASE_AUTH_EMULATOR_HOST ||
      !process.env.FIRESTORE_EMULATOR_HOST) {
    throw new Error(
      "Refusing to seed: both FIREBASE_AUTH_EMULATOR_HOST and " +
      "FIRESTORE_EMULATOR_HOST must be set.",
    );
  }
}

async function upsertAuthEmployees(): Promise<void> {
  const auth = getAuth();
  for (const employee of EMPLOYEES) {
    try {
      await auth.getUser(employee.uid);
      await auth.updateUser(employee.uid, {
        email: employee.email,
        displayName: employee.displayName,
        password: PASSWORD,
        emailVerified: true,
        disabled: false,
      });
    } catch (error) {
      if ((error as { code?: string }).code !== "auth/user-not-found") {
        throw error;
      }
      await auth.createUser({
        uid: employee.uid,
        email: employee.email,
        displayName: employee.displayName,
        password: PASSWORD,
        emailVerified: true,
        disabled: false,
      });
    }
    await auth.setCustomUserClaims(employee.uid, {
      orgId: ORG_ID,
      role: employee.role,
    });
  }
}

function memberDocument(employee: SeedEmployee, now: Timestamp): DocumentData {
  return {
    email: employee.email,
    displayName: employee.displayName,
    role: employee.role,
    active: true,
    avatarKey: employee.avatarKey,
    notificationPreference: "ALL",
    lastSeenAt: now,
    activeUntil: null,
    createdAt: now,
    updatedAt: now,
  };
}

function membershipDocument(
  uid: string,
  role: "ADMIN" | "MEMBER",
  now: Timestamp,
): DocumentData {
  return {
    userId: uid,
    role,
    joinedAt: now,
    lastDeliveredAt: EPOCH,
    lastReadAt: EPOCH,
    updatedAt: now,
  };
}

function messageDocument(
  senderId: string,
  text: string,
  createdAt: Timestamp,
  replyToMessageId: string | null = null,
): DocumentData {
  return {
    senderId,
    text,
    clientCreatedAt: createdAt,
    serverCreatedAt: createdAt,
    replyToMessageId,
    deletedAt: null,
    schemaVersion: 1,
  };
}

function inboxDocument(
  conversationId: string,
  conversationType: "DIRECT" | "GROUP",
  title: string,
  lastMessageId: string,
  lastMessagePreview: string,
  lastMessageSenderId: string,
  lastMessageAt: Timestamp,
  unreadCount: number,
): DocumentData {
  return {
    conversationId,
    orgId: ORG_ID,
    conversationType,
    title,
    lastMessageId,
    lastMessagePreview,
    lastMessageSenderId,
    lastMessageAt,
    unreadCount,
    muted: false,
    pinned: false,
    updatedAt: lastMessageAt,
  };
}

async function seedFirestore(db: Firestore): Promise<{
  directConversationId: string;
  groupConversationId: string;
}> {
  const createdAt = Timestamp.fromDate(new Date("2026-08-11T06:00:00.000Z"));
  const directId = directConversationId("seed-own", "seed-mia");
  const groupId = "seed-innovfix-team";
  const directMembers = ["seed-mia", "seed-own"];
  const groupMembers = EMPLOYEES.map((employee) => employee.uid).sort();
  const directMessage1 = "019892be-1000-7000-8000-000000000001";
  const directMessage2 = "019892be-2000-7000-8000-000000000002";
  const directMessage3 = "019892be-3000-7000-8000-000000000003";
  const groupMessage1 = "019892bf-1000-7000-8000-000000000001";
  const groupMessage2 = "019892bf-2000-7000-8000-000000000002";
  const groupMessage3 = "019892bf-3000-7000-8000-000000000003";
  const t1 = Timestamp.fromMillis(createdAt.toMillis() + 1_000);
  const t2 = Timestamp.fromMillis(createdAt.toMillis() + 2_000);
  const t3 = Timestamp.fromMillis(createdAt.toMillis() + 3_000);

  const batch = db.batch();
  EMPLOYEES.forEach((employee) => {
    batch.set(
      db.doc("organizations/" + ORG_ID + "/members/" + employee.uid),
      memberDocument(employee, createdAt),
    );
  });

  const directRef = db.collection("conversations").doc(directId);
  batch.set(directRef, {
    orgId: ORG_ID,
    type: "DIRECT",
    memberIds: directMembers,
    title: null,
    createdBy: "seed-own",
    createdAt,
    updatedAt: t3,
    schemaVersion: 1,
  });
  directMembers.forEach((uid) => {
    batch.set(
      directRef.collection("members").doc(uid),
      membershipDocument(uid, "MEMBER", createdAt),
    );
  });
  batch.set(
    directRef.collection("messages").doc(directMessage1),
    messageDocument("seed-own", "Reimbursement", t1),
  );
  batch.set(
    directRef.collection("messages").doc(directMessage2),
    messageDocument("seed-mia", "Could you send the updated ID card?", t2),
  );
  batch.set(
    directRef.collection("messages").doc(directMessage3),
    messageDocument("seed-own", "Sure, I will send it today.", t3, directMessage2),
  );
  batch.set(
    directRef.collection("reactions").doc(directMessage2 + "_seed-own"),
    {
      messageId: directMessage2,
      userId: "seed-own",
      emoji: "👍",
      createdAt: t3,
      updatedAt: t3,
    },
  );
  batch.set(
    db.doc("members/seed-own/inbox/" + directId),
    inboxDocument(
      directId,
      "DIRECT",
      "Mia Kapoor",
      directMessage3,
      "Sure, I will send it today.",
      "seed-own",
      t3,
      0,
    ),
  );
  batch.set(
    db.doc("members/seed-mia/inbox/" + directId),
    inboxDocument(
      directId,
      "DIRECT",
      "Northstar Studio",
      directMessage3,
      "Sure, I will send it today.",
      "seed-own",
      t3,
      1,
    ),
  );

  const groupRef = db.collection("conversations").doc(groupId);
  batch.set(groupRef, {
    orgId: ORG_ID,
    type: "GROUP",
    memberIds: groupMembers,
    title: "INNOVFIX",
    createdBy: "seed-own",
    createdAt,
    updatedAt: t3,
    schemaVersion: 1,
  });
  groupMembers.forEach((uid) => {
    batch.set(
      groupRef.collection("members").doc(uid),
      membershipDocument(uid, uid === "seed-own" ? "ADMIN" : "MEMBER", createdAt),
    );
  });
  batch.set(
    groupRef.collection("messages").doc(groupMessage1),
    messageDocument(
      "seed-dev",
      "Please make sure the content is concise and ready for client review.",
      t1,
    ),
  );
  batch.set(
    groupRef.collection("messages").doc(groupMessage2),
    messageDocument("seed-own", "Sure, I will update it today.", t2, groupMessage1),
  );
  batch.set(
    groupRef.collection("messages").doc(groupMessage3),
    messageDocument("seed-reena", "The visual direction looks good.", t3),
  );
  EMPLOYEES.forEach((employee) => {
    batch.set(
      db.doc("members/" + employee.uid + "/inbox/" + groupId),
      inboxDocument(
        groupId,
        "GROUP",
        "INNOVFIX",
        groupMessage3,
        "The visual direction looks good.",
        "seed-reena",
        t3,
        employee.uid === "seed-reena" ? 0 : 1,
      ),
    );
  });

  await batch.commit();
  return { directConversationId: directId, groupConversationId: groupId };
}

async function main(): Promise<void> {
  assertEmulatorOnly();
  if (getApps().length === 0) {
    initializeApp({ projectId: PROJECT_ID });
  }
  await upsertAuthEmployees();
  const conversations = await seedFirestore(getFirestore());
  process.stdout.write(JSON.stringify({
    projectId: PROJECT_ID,
    organizationId: ORG_ID,
    employees: EMPLOYEES.map(({ uid, email, displayName, role }) => ({
      uid,
      email,
      displayName,
      role,
    })),
    emulatorPassword: PASSWORD,
    ...conversations,
  }, null, 2) + "\n");
}

void main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : "Unknown emulator seed failure.";
  process.stderr.write("Emulator seed failed: " + message + "\n");
  process.exitCode = 1;
});
