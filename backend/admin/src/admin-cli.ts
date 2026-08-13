import {
  applicationDefault,
  getApps,
  initializeApp,
} from "firebase-admin/app";
import { getAuth, type UserRecord } from "firebase-admin/auth";
import {
  FieldValue,
  Timestamp,
  getFirestore,
  type DocumentData,
  type DocumentReference,
  type Firestore,
} from "firebase-admin/firestore";
import {
  boundedText,
  conversationRole,
  emailAddress,
  identifier,
  identifierList,
  option,
  organizationRole,
  parseCommandLine,
} from "./domain";

const ORG_ID = "innovfix";
const SCHEMA_VERSION = 1;
const EPOCH = Timestamp.fromMillis(0);

interface Context {
  db: Firestore;
  projectId: string;
}

const USAGE =
  "INNOVFIX Firebase administrator CLI\n\n" +
  "Build first:\n" +
  "  npm run build\n\n" +
  "Commands:\n" +
  "  node lib/admin-cli.js invite --project PROJECT --confirm-project PROJECT " +
  "--email EMAIL --name NAME [--role MEMBER|ADMIN] [--uid UID] [--avatar-key KEY]\n" +
  "  node lib/admin-cli.js disable --project PROJECT --confirm-project PROJECT --uid UID\n" +
  "  node lib/admin-cli.js enable --project PROJECT --confirm-project PROJECT --uid UID\n" +
  "  node lib/admin-cli.js role --project PROJECT --confirm-project PROJECT " +
  "--uid UID --role MEMBER|ADMIN\n" +
  "  node lib/admin-cli.js create-group --project PROJECT --confirm-project PROJECT " +
  "--title TITLE --owner UID --members UID,UID [--admins UID,UID] [--id GROUP_ID]\n" +
  "  node lib/admin-cli.js add-member --project PROJECT --confirm-project PROJECT " +
  "--group GROUP_ID --uid UID [--role MEMBER|ADMIN]\n" +
  "  node lib/admin-cli.js remove-member --project PROJECT --confirm-project PROJECT " +
  "--group GROUP_ID --uid UID\n" +
  "  node lib/admin-cli.js set-group-role --project PROJECT --confirm-project PROJECT " +
  "--group GROUP_ID --uid UID --role MEMBER|ADMIN\n\n" +
  "Production commands require Application Default Credentials and an exact " +
  "--confirm-project value. With both Firebase emulator host variables set, " +
  "--confirm-project is not required.";

function requireKnownOptions(
  options: Record<string, string | boolean>,
  allowed: string[],
): void {
  const common = new Set(["project", "confirm-project", ...allowed]);
  const unknown = Object.keys(options).filter((key) => !common.has(key));
  if (unknown.length > 0) {
    throw new Error("Unknown option(s): " + unknown.map((key) => "--" + key).join(", "));
  }
}

function projectContext(options: Record<string, string | boolean>): Context {
  const authEmulator = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  const firestoreEmulator = process.env.FIRESTORE_EMULATOR_HOST;
  if (Boolean(authEmulator) !== Boolean(firestoreEmulator)) {
    throw new Error(
      "Set both FIREBASE_AUTH_EMULATOR_HOST and FIRESTORE_EMULATOR_HOST, or neither.",
    );
  }
  const projectId = identifier(
    option(options, "project", false) ??
      process.env.GOOGLE_CLOUD_PROJECT ??
      process.env.GCLOUD_PROJECT,
    "project",
  );
  const usesEmulators = Boolean(authEmulator && firestoreEmulator);
  if (!usesEmulators && option(options, "confirm-project") !== projectId) {
    throw new Error(
      "Production mutation refused: --confirm-project must exactly match --project.",
    );
  }

  if (getApps().length === 0) {
    initializeApp(
      usesEmulators
        ? { projectId }
        : { credential: applicationDefault(), projectId },
    );
  }
  return { db: getFirestore(), projectId };
}

function organizationMemberRef(db: Firestore, uid: string): DocumentReference {
  return db.doc("organizations/" + ORG_ID + "/members/" + uid);
}

function privateMemberRef(db: Firestore, uid: string): DocumentReference {
  return db.doc("members/" + uid);
}

function conversationRef(db: Firestore, id: string): DocumentReference {
  return db.collection("conversations").doc(id);
}

function assertActiveProfile(
  snapshot: FirebaseFirestore.DocumentSnapshot,
  uid: string,
): DocumentData {
  if (!snapshot.exists || snapshot.data()?.active !== true) {
    throw new Error("Employee " + uid + " does not exist or is inactive.");
  }
  return snapshot.data()!;
}

function assertGroup(
  snapshot: FirebaseFirestore.DocumentSnapshot,
  groupId: string,
): DocumentData {
  const data = snapshot.data();
  if (!snapshot.exists || data?.orgId !== ORG_ID || data.type !== "GROUP" ||
      !Array.isArray(data.memberIds)) {
    throw new Error("Group " + groupId + " does not exist or has an invalid schema.");
  }
  return data;
}

async function invite(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["email", "name", "role", "uid", "avatar-key"]);
  const email = emailAddress(option(options, "email"));
  const displayName = boundedText(option(options, "name"), "name", 80);
  const role = organizationRole(option(options, "role", false));
  const requestedUid = option(options, "uid", false);
  const uid = requestedUid == null ? undefined : identifier(requestedUid, "uid");
  const avatarKeyOption = option(options, "avatar-key", false);
  const avatarKey = avatarKeyOption == null
    ? null
    : boundedText(avatarKeyOption, "avatar-key", 120);
  const auth = getAuth();

  let user: UserRecord | undefined;
  try {
    user = await auth.createUser({
      uid,
      email,
      displayName,
      emailVerified: false,
      disabled: false,
    });
    await auth.setCustomUserClaims(user.uid, { orgId: ORG_ID, role });
    const now = Timestamp.now();
    await organizationMemberRef(context.db, user.uid).create({
      email,
      displayName,
      role,
      active: true,
      avatarKey,
      notificationPreference: "ALL",
      lastSeenAt: null,
      activeUntil: null,
      createdAt: now,
      updatedAt: now,
    });
    const passwordSetupLink = await auth.generatePasswordResetLink(email);
    process.stdout.write(JSON.stringify({
      command: "invite",
      projectId: context.projectId,
      uid: user.uid,
      email,
      role,
      passwordSetupLink,
    }, null, 2) + "\n");
  } catch (error) {
    if (user != null) {
      await Promise.allSettled([
        auth.deleteUser(user.uid),
        organizationMemberRef(context.db, user.uid).delete(),
      ]);
    }
    throw error;
  }
}

async function setEmployeeEnabled(
  context: Context,
  options: Record<string, string | boolean>,
  enabled: boolean,
): Promise<void> {
  requireKnownOptions(options, ["uid"]);
  const uid = identifier(option(options, "uid"), "uid");
  const auth = getAuth();
  await auth.updateUser(uid, { disabled: !enabled });
  if (!enabled) {
    await auth.revokeRefreshTokens(uid);
  }
  await organizationMemberRef(context.db, uid).update({
    active: enabled,
    activeUntil: null,
    updatedAt: Timestamp.now(),
  });
  if (!enabled) {
    const devices = await privateMemberRef(context.db, uid).collection("devices").get();
    const writer = context.db.bulkWriter();
    const now = Timestamp.now();
    devices.docs.forEach((device) => {
      writer.set(device.ref, { enabled: false, updatedAt: now }, { merge: true });
    });
    await writer.close();
  }
  process.stdout.write(JSON.stringify({
    command: enabled ? "enable" : "disable",
    projectId: context.projectId,
    uid,
    active: enabled,
  }, null, 2) + "\n");
}

async function assignOrganizationRole(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["uid", "role"]);
  const uid = identifier(option(options, "uid"), "uid");
  const role = organizationRole(option(options, "role"));
  const auth = getAuth();
  const user = await auth.getUser(uid);
  await auth.setCustomUserClaims(uid, {
    ...(user.customClaims ?? {}),
    orgId: ORG_ID,
    role,
  });
  await organizationMemberRef(context.db, uid).update({
    role,
    updatedAt: Timestamp.now(),
  });
  await auth.revokeRefreshTokens(uid);
  process.stdout.write(JSON.stringify({
    command: "role",
    projectId: context.projectId,
    uid,
    role,
  }, null, 2) + "\n");
}

async function requireActiveEmployees(db: Firestore, userIds: string[]): Promise<void> {
  const snapshots = await db.getAll(
    ...userIds.map((uid) => organizationMemberRef(db, uid)),
  );
  snapshots.forEach((snapshot, index) => {
    assertActiveProfile(snapshot, userIds[index]);
  });
}

function emptyGroupInbox(
  groupId: string,
  title: string,
  now: Timestamp,
): DocumentData {
  return {
    conversationId: groupId,
    orgId: ORG_ID,
    conversationType: "GROUP",
    title,
    lastMessageId: null,
    lastMessagePreview: "",
    lastMessageSenderId: null,
    lastMessageAt: null,
    unreadCount: 0,
    muted: false,
    pinned: false,
    updatedAt: now,
  };
}

async function createGroup(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["title", "owner", "members", "admins", "id"]);
  const title = boundedText(option(options, "title"), "title", 80);
  const owner = identifier(option(options, "owner"), "owner");
  const members = identifierList(option(options, "members"), "members");
  const adminsOption = option(options, "admins", false);
  const admins = adminsOption == null ? [] : identifierList(adminsOption, "admins");
  const allMembers = [...new Set([owner, ...members, ...admins])].sort();
  if (allMembers.length > 100) {
    throw new Error("A group can contain at most 100 employees.");
  }
  await requireActiveEmployees(context.db, allMembers);
  const requestedId = option(options, "id", false);
  const ref = requestedId == null
    ? context.db.collection("conversations").doc()
    : conversationRef(context.db, identifier(requestedId, "id"));
  const now = Timestamp.now();
  const batch = context.db.batch();
  batch.create(ref, {
    orgId: ORG_ID,
    type: "GROUP",
    memberIds: allMembers,
    title,
    createdBy: owner,
    createdAt: now,
    updatedAt: now,
    schemaVersion: SCHEMA_VERSION,
  });
  allMembers.forEach((uid) => {
    const role = uid === owner || admins.includes(uid) ? "ADMIN" : "MEMBER";
    batch.create(ref.collection("members").doc(uid), {
      userId: uid,
      role,
      joinedAt: now,
      lastDeliveredAt: EPOCH,
      lastReadAt: EPOCH,
      updatedAt: now,
    });
    batch.create(
      privateMemberRef(context.db, uid).collection("inbox").doc(ref.id),
      emptyGroupInbox(ref.id, title, now),
    );
  });
  await batch.commit();
  process.stdout.write(JSON.stringify({
    command: "create-group",
    projectId: context.projectId,
    conversationId: ref.id,
    title,
    memberCount: allMembers.length,
  }, null, 2) + "\n");
}

async function addMember(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["group", "uid", "role"]);
  const groupId = identifier(option(options, "group"), "group");
  const uid = identifier(option(options, "uid"), "uid");
  const role = conversationRole(option(options, "role", false));
  const profileSnapshot = await organizationMemberRef(context.db, uid).get();
  assertActiveProfile(profileSnapshot, uid);
  const groupRef = conversationRef(context.db, groupId);
  const memberRef = groupRef.collection("members").doc(uid);
  const userInboxRef = privateMemberRef(context.db, uid).collection("inbox").doc(groupId);

  await context.db.runTransaction(async (transaction): Promise<void> => {
    const [groupSnapshot, membershipSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
    ]);
    const group = assertGroup(groupSnapshot, groupId);
    if (membershipSnapshot.exists || group.memberIds.includes(uid)) {
      throw new Error("Employee " + uid + " is already a member of " + groupId + ".");
    }
    if (group.memberIds.length >= 100) {
      throw new Error("A group can contain at most 100 employees.");
    }
    const now = Timestamp.now();
    transaction.update(groupRef, {
      memberIds: FieldValue.arrayUnion(uid),
      updatedAt: now,
    });
    transaction.create(memberRef, {
      userId: uid,
      role,
      joinedAt: now,
      lastDeliveredAt: EPOCH,
      lastReadAt: EPOCH,
      updatedAt: now,
    });
    transaction.set(userInboxRef, emptyGroupInbox(groupId, group.title, now));
  });
  process.stdout.write(JSON.stringify({
    command: "add-member",
    projectId: context.projectId,
    groupId,
    uid,
    role,
  }, null, 2) + "\n");
}

async function removeMember(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["group", "uid"]);
  const groupId = identifier(option(options, "group"), "group");
  const uid = identifier(option(options, "uid"), "uid");
  const groupRef = conversationRef(context.db, groupId);
  const memberRef = groupRef.collection("members").doc(uid);
  const userInboxRef = privateMemberRef(context.db, uid).collection("inbox").doc(groupId);

  await context.db.runTransaction(async (transaction): Promise<void> => {
    const [groupSnapshot, membershipSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
    ]);
    const group = assertGroup(groupSnapshot, groupId);
    if (!membershipSnapshot.exists || !group.memberIds.includes(uid)) {
      throw new Error("Employee " + uid + " is not a member of " + groupId + ".");
    }
    if (group.createdBy === uid) {
      throw new Error("The group creator cannot be removed.");
    }
    const now = Timestamp.now();
    transaction.update(groupRef, {
      memberIds: FieldValue.arrayRemove(uid),
      updatedAt: now,
    });
    transaction.delete(memberRef);
    transaction.delete(userInboxRef);
  });
  process.stdout.write(JSON.stringify({
    command: "remove-member",
    projectId: context.projectId,
    groupId,
    uid,
  }, null, 2) + "\n");
}

async function setGroupRole(
  context: Context,
  options: Record<string, string | boolean>,
): Promise<void> {
  requireKnownOptions(options, ["group", "uid", "role"]);
  const groupId = identifier(option(options, "group"), "group");
  const uid = identifier(option(options, "uid"), "uid");
  const role = conversationRole(option(options, "role"));
  const groupRef = conversationRef(context.db, groupId);
  const memberRef = groupRef.collection("members").doc(uid);
  await context.db.runTransaction(async (transaction): Promise<void> => {
    const [groupSnapshot, memberSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
    ]);
    const group = assertGroup(groupSnapshot, groupId);
    if (!memberSnapshot.exists) {
      throw new Error("Employee " + uid + " is not a member of " + groupId + ".");
    }
    if (group.createdBy === uid) {
      throw new Error("The group creator must remain an administrator.");
    }
    transaction.update(memberRef, { role, updatedAt: Timestamp.now() });
  });
  process.stdout.write(JSON.stringify({
    command: "set-group-role",
    projectId: context.projectId,
    groupId,
    uid,
    role,
  }, null, 2) + "\n");
}

async function main(): Promise<void> {
  const { command, options } = parseCommandLine(process.argv.slice(2));
  if (command === "help") {
    process.stdout.write(USAGE + "\n");
    return;
  }
  const context = projectContext(options);
  switch (command) {
    case "invite":
      await invite(context, options);
      break;
    case "disable":
      await setEmployeeEnabled(context, options, false);
      break;
    case "enable":
      await setEmployeeEnabled(context, options, true);
      break;
    case "role":
      await assignOrganizationRole(context, options);
      break;
    case "create-group":
      await createGroup(context, options);
      break;
    case "add-member":
      await addMember(context, options);
      break;
    case "remove-member":
      await removeMember(context, options);
      break;
    case "set-group-role":
      await setGroupRole(context, options);
      break;
    default:
      throw new Error("Unknown command: " + command + "\n\n" + USAGE);
  }
}

void main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : "Unknown administrator CLI failure.";
  process.stderr.write("Admin command failed: " + message + "\n");
  process.exitCode = 1;
});
