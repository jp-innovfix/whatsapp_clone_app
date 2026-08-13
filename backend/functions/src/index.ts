import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth, type UserRecord } from "firebase-admin/auth";
import {
  FieldValue,
  Timestamp,
  getFirestore,
  type DocumentData,
  type DocumentReference,
} from "firebase-admin/firestore";
import { getMessaging, type Message } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { setGlobalOptions } from "firebase-functions/v2";
import {
  onDocumentCreated,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { defineBoolean } from "firebase-functions/params";
import {
  MAX_MESSAGE_LENGTH,
  ORG_ID,
  REGION,
  SCHEMA_VERSION,
  type ConversationDocument,
  type ConversationMemberDocument,
  type AnonymousBootstrapResponse,
  type DirectConversationResponse,
  type GroupConversationResponse,
  type InboxDocument,
  type MessageDocument,
  type OrganizationMemberDocument,
} from "./contracts";
import {
  DELETED_MESSAGE_PREVIEW,
  DIRECT,
  GROUP,
  becameDeleted,
  canSendNotification,
  createdMessageEffects,
  directConversationId,
  displayName,
  hasReadThrough,
  idempotencyKey,
  isPermanentFcmTokenError,
  messagePreview,
  notificationTitle,
  nextUnreadCount,
  recipientIds,
  shouldReplaceMessageSummary,
  unreadCountAfterReceipt,
} from "./domain";

if (getApps().length === 0) {
  initializeApp();
}

const db = getFirestore();
db.settings({ ignoreUndefinedProperties: true });

setGlobalOptions({
  region: REGION,
  memory: "256MiB",
  timeoutSeconds: 60,
  maxInstances: 20,
  concurrency: 80,
});

const EPOCH = Timestamp.fromMillis(0);
const LEDGER_TTL_MILLIS = 7 * 24 * 60 * 60 * 1_000;
const MESSAGE_DOCUMENT = "conversations/{conversationId}/messages/{messageId}";
const CONVERSATION_MEMBER_DOCUMENT = "conversations/{conversationId}/members/{uid}";
const DEMO_EMPLOYEE_EMAIL = "ayush@innovfix.in";
const DEMO_EMPLOYEE_NAME = "Ayush Co founder";
const DEMO_EMPLOYEE_UID = "demo-ayush";
const enforceAppCheck = defineBoolean("ENFORCE_APP_CHECK", {
  default: false,
  description: "Reject callable requests without valid App Check after the pilot.",
});

interface FunctionEventDocument {
  eventId: string;
  eventType: "MESSAGE_CREATED" | "MESSAGE_DELETED";
  conversationId: string;
  messageId: string;
  fanoutCompletedAt: Timestamp;
  notificationAttemptedAt: Timestamp | null;
  notificationCompletedAt: Timestamp | null;
  notificationLeaseUntil: Timestamp;
  completedDevicePaths: string[];
  expireAt: Timestamp;
}

interface NotificationCandidate {
  devicePath: string;
  token: string;
  message: Message;
}

function organizationMemberRef(uid: string): DocumentReference {
  return db.doc("organizations/" + ORG_ID + "/members/" + uid);
}

function privateMemberRef(uid: string): DocumentReference {
  return db.doc("members/" + uid);
}

function inboxRef(uid: string, conversationId: string): DocumentReference {
  return privateMemberRef(uid).collection("inbox").doc(conversationId);
}

function activeMember(
  snapshot: FirebaseFirestore.DocumentSnapshot,
): OrganizationMemberDocument | null {
  if (!snapshot.exists) {
    return null;
  }
  const data = snapshot.data() as OrganizationMemberDocument;
  return data.active === true ? data : null;
}

function requireCallerOrg(request: { auth?: { token: Record<string, unknown> } | null }): void {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in before creating a conversation.");
  }
  if (request.auth.token.orgId !== ORG_ID) {
    throw new HttpsError("permission-denied", "Your account is not part of this organization.");
  }
}

function memberDocument(
  email: string,
  displayNameValue: string,
  now: Timestamp,
): OrganizationMemberDocument {
  return {
    email,
    displayName: displayNameValue,
    role: "MEMBER",
    active: true,
    avatarKey: null,
    notificationPreference: "ALL",
    lastSeenAt: null,
    activeUntil: null,
    createdAt: now,
    updatedAt: now,
  };
}

async function ensureDemoEmployee(): Promise<UserRecord> {
  const auth = getAuth();
  let user: UserRecord;
  try {
    user = await auth.getUserByEmail(DEMO_EMPLOYEE_EMAIL);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
    try {
      user = await auth.createUser({
        uid: DEMO_EMPLOYEE_UID,
        email: DEMO_EMPLOYEE_EMAIL,
        emailVerified: false,
        displayName: DEMO_EMPLOYEE_NAME,
        disabled: false,
      });
    } catch (creationError) {
      const code = (creationError as { code?: string }).code;
      if (code !== "auth/email-already-exists" && code !== "auth/uid-already-exists") {
        throw creationError;
      }
      user = await auth.getUserByEmail(DEMO_EMPLOYEE_EMAIL);
    }
  }
  await auth.setCustomUserClaims(user.uid, {
    ...(user.customClaims ?? {}),
    orgId: ORG_ID,
    role: "MEMBER",
    demo: true,
  });
  return user;
}

export const bootstrapAnonymousMember = onCall(
  { enforceAppCheck },
  async (request): Promise<AnonymousBootstrapResponse> => {
    if (request.auth == null || request.auth.token.firebase?.sign_in_provider !== "anonymous") {
      throw new HttpsError("unauthenticated", "Anonymous Firebase sign-in is required.");
    }
    const callerUid = request.auth.uid;
    const auth = getAuth();
    const [callerUser, demoEmployee] = await Promise.all([
      auth.getUser(callerUid),
      ensureDemoEmployee(),
    ]);
    await auth.setCustomUserClaims(callerUid, {
      ...(callerUser.customClaims ?? {}),
      orgId: ORG_ID,
      role: "MEMBER",
      anonymous: true,
    });

    const now = Timestamp.now();
    const callerName = "INNOVFIX Tester " + callerUid.slice(0, 6).toUpperCase();
    const callerProfile = organizationMemberRef(callerUid);
    const demoProfile = organizationMemberRef(demoEmployee.uid);
    const conversationId = directConversationId(ORG_ID, callerUid, demoEmployee.uid);
    const directRef = db.collection("conversations").doc(conversationId);
    const callerMembership = directRef.collection("members").doc(callerUid);
    const demoMembership = directRef.collection("members").doc(demoEmployee.uid);
    const callerInbox = inboxRef(callerUid, conversationId);
    const demoInbox = inboxRef(demoEmployee.uid, conversationId);
    await db.runTransaction(async (transaction): Promise<void> => {
      const [
        callerSnapshot,
        demoSnapshot,
        conversationSnapshot,
        callerMembershipSnapshot,
        demoMembershipSnapshot,
        callerInboxSnapshot,
        demoInboxSnapshot,
      ] = await Promise.all([
        transaction.get(callerProfile),
        transaction.get(demoProfile),
        transaction.get(directRef),
        transaction.get(callerMembership),
        transaction.get(demoMembership),
        transaction.get(callerInbox),
        transaction.get(demoInbox),
      ]);
      if (!callerSnapshot.exists) {
        transaction.create(callerProfile, memberDocument("anonymous@innovfix.in", callerName, now));
      } else {
        transaction.update(callerProfile, { active: true, updatedAt: now });
      }
      if (!demoSnapshot.exists) {
        transaction.create(demoProfile, memberDocument(DEMO_EMPLOYEE_EMAIL, DEMO_EMPLOYEE_NAME, now));
      } else {
        transaction.update(demoProfile, {
          email: DEMO_EMPLOYEE_EMAIL,
          displayName: DEMO_EMPLOYEE_NAME,
          active: true,
          updatedAt: now,
        });
      }
      const conversation: ConversationDocument = conversationSnapshot.exists
        ? assertConversation(conversationSnapshot.data(), conversationId)
        : {
          orgId: ORG_ID,
          type: DIRECT,
          memberIds: [callerUid, demoEmployee.uid].sort(),
          title: null,
          createdBy: callerUid,
          createdAt: now,
          updatedAt: now,
          schemaVersion: SCHEMA_VERSION,
        };
      if (!conversationSnapshot.exists) {
        transaction.create(directRef, conversation);
      }
      const membership = (uid: string): ConversationMemberDocument => ({
        userId: uid,
        role: "MEMBER",
        joinedAt: now,
        lastDeliveredAt: EPOCH,
        lastReadAt: EPOCH,
        updatedAt: now,
      });
      if (!callerMembershipSnapshot.exists) {
        transaction.create(callerMembership, membership(callerUid));
      }
      if (!demoMembershipSnapshot.exists) {
        transaction.create(demoMembership, membership(demoEmployee.uid));
      }
      if (!callerInboxSnapshot.exists) {
        transaction.create(
          callerInbox,
          baseInbox(conversationId, conversation, DEMO_EMPLOYEE_NAME, now),
        );
      }
      if (!demoInboxSnapshot.exists) {
        transaction.create(
          demoInbox,
          baseInbox(conversationId, conversation, callerName, now),
        );
      }
    });
    return { userId: callerUid, demoEmployeeId: demoEmployee.uid };
  },
);

function readTargetUserId(data: unknown): string {
  if (typeof data !== "object" || data == null ||
      typeof (data as Record<string, unknown>).targetUserId !== "string") {
    throw new HttpsError("invalid-argument", "targetUserId must be a Firebase user ID.");
  }
  const targetUserId = ((data as Record<string, unknown>).targetUserId as string).trim();
  if (targetUserId.length === 0 || targetUserId.length > 128 || targetUserId.includes("/")) {
    throw new HttpsError("invalid-argument", "targetUserId must be a valid Firebase user ID.");
  }
  return targetUserId;
}

function assertConversation(
  value: DocumentData | undefined,
  conversationId: string,
): ConversationDocument {
  if (value == null || value.orgId !== ORG_ID ||
      (value.type !== DIRECT && value.type !== GROUP) ||
      !Array.isArray(value.memberIds) || value.memberIds.length === 0) {
    throw new Error("Conversation " + conversationId + " has an invalid server schema.");
  }
  return value as ConversationDocument;
}

function assertMessage(value: DocumentData, messageId: string): MessageDocument {
  const kind = value.kind ?? "TEXT";
  const typedContentValid = kind === "TEXT"
    ? typeof value.text === "string" && value.text.length > 0 && value.text.length <= MAX_MESSAGE_LENGTH
    : ["IMAGE", "DOCUMENT", "AUDIO"].includes(kind) &&
      typeof value.storagePath === "string" && value.storagePath.length > 0 &&
      typeof value.fileName === "string" && value.fileName.length > 0;
  if (typeof value.senderId !== "string" || typeof value.text !== "string" ||
      !typedContentValid ||
      !(value.clientCreatedAt instanceof Timestamp) ||
      !(value.serverCreatedAt instanceof Timestamp) ||
      value.schemaVersion !== SCHEMA_VERSION) {
    throw new Error("Message " + messageId + " has an invalid server schema.");
  }
  return value as MessageDocument;
}

function profileTitle(
  conversation: ConversationDocument,
  viewerUid: string,
  profiles: Map<string, OrganizationMemberDocument>,
): string {
  if (conversation.type === GROUP) {
    return displayName(conversation.title);
  }
  const otherUid = conversation.memberIds.find((uid) => uid !== viewerUid);
  return displayName(otherUid == null ? null : profiles.get(otherUid)?.displayName);
}

function baseInbox(
  conversationId: string,
  conversation: ConversationDocument,
  title: string,
  now: Timestamp,
): InboxDocument {
  return {
    conversationId,
    orgId: ORG_ID,
    conversationType: conversation.type,
    title,
    lastMessageId: null,
    lastMessagePreview: "",
    lastMessageSenderId: null,
    lastMessageAt: null,
    unreadCount: 0,
    muted: false,
    pinned: false,
    archived: false,
    updatedAt: now,
  };
}

export const getOrCreateDirectConversation = onCall(
  { enforceAppCheck },
  async (request): Promise<DirectConversationResponse> => {
    requireCallerOrg(request);
    const callerUid = request.auth!.uid;
    const targetUserId = readTargetUserId(request.data);
    if (targetUserId === callerUid) {
      throw new HttpsError("invalid-argument", "Self conversations are not supported in messaging v1.");
    }

    const conversationId = directConversationId(ORG_ID, callerUid, targetUserId);
    const conversationRef = db.collection("conversations").doc(conversationId);

    return db.runTransaction(async (transaction): Promise<DirectConversationResponse> => {
      const [callerSnapshot, targetSnapshot, conversationSnapshot] = await Promise.all([
        transaction.get(organizationMemberRef(callerUid)),
        transaction.get(organizationMemberRef(targetUserId)),
        transaction.get(conversationRef),
      ]);
      const caller = activeMember(callerSnapshot);
      const target = activeMember(targetSnapshot);
      if (caller == null) {
        throw new HttpsError("permission-denied", "Your employee account is inactive.");
      }
      if (target == null) {
        throw new HttpsError("not-found", "The selected employee is unavailable.");
      }

      if (conversationSnapshot.exists) {
        const existing = assertConversation(conversationSnapshot.data(), conversationId);
        const expected = [callerUid, targetUserId].sort();
        const actual = [...existing.memberIds].sort();
        if (existing.type !== DIRECT || actual.join(":") !== expected.join(":")) {
          throw new HttpsError("failed-precondition", "The direct conversation ID is already in use.");
        }
        return { conversationId, created: false };
      }

      const now = Timestamp.now();
      const conversation: ConversationDocument = {
        orgId: ORG_ID,
        type: DIRECT,
        memberIds: [callerUid, targetUserId].sort(),
        title: null,
        createdBy: callerUid,
        createdAt: now,
        updatedAt: now,
        schemaVersion: SCHEMA_VERSION,
      };
      const conversationMember = (uid: string): ConversationMemberDocument => ({
        userId: uid,
        role: "MEMBER",
        joinedAt: now,
        lastDeliveredAt: EPOCH,
        lastReadAt: EPOCH,
        updatedAt: now,
      });

      transaction.create(conversationRef, conversation);
      transaction.create(conversationRef.collection("members").doc(callerUid), conversationMember(callerUid));
      transaction.create(conversationRef.collection("members").doc(targetUserId), conversationMember(targetUserId));
      transaction.create(
        inboxRef(callerUid, conversationId),
        baseInbox(conversationId, conversation, displayName(target.displayName), now),
      );
      transaction.create(
        inboxRef(targetUserId, conversationId),
        baseInbox(conversationId, conversation, displayName(caller.displayName), now),
      );
      return { conversationId, created: true };
    });
  },
);

export const createGroupConversation = onCall(
  { enforceAppCheck },
  async (request): Promise<GroupConversationResponse> => {
    requireCallerOrg(request);
    const callerUid = request.auth!.uid;
    const data = request.data as { title?: unknown; memberIds?: unknown };
    const title = typeof data?.title === "string" ? data.title.trim() : "";
    const requestedIds = Array.isArray(data?.memberIds)
      ? data.memberIds.filter((value): value is string => typeof value === "string")
      : [];
    const memberIds = [...new Set([callerUid, ...requestedIds.map((value) => value.trim())])]
      .filter((value) => value.length > 0 && value.length <= 128 && !value.includes("/"))
      .sort();
    if (title.length < 1 || title.length > 120) {
      throw new HttpsError("invalid-argument", "Group name must contain 1 to 120 characters.");
    }
    if (memberIds.length < 2 || memberIds.length > 100) {
      throw new HttpsError("invalid-argument", "Select between 1 and 99 employees.");
    }
    const profiles = await Promise.all(memberIds.map((uid) => organizationMemberRef(uid).get()));
    if (profiles.some((snapshot) => activeMember(snapshot) == null)) {
      throw new HttpsError("failed-precondition", "Every group member must be an active employee.");
    }
    const conversationRef = db.collection("conversations").doc();
    const now = Timestamp.now();
    const conversation: ConversationDocument = {
      orgId: ORG_ID,
      type: GROUP,
      memberIds,
      title,
      createdBy: callerUid,
      createdAt: now,
      updatedAt: now,
      schemaVersion: SCHEMA_VERSION,
    };
    await db.runTransaction(async (transaction): Promise<void> => {
      transaction.create(conversationRef, conversation);
      memberIds.forEach((uid) => {
        transaction.create(conversationRef.collection("members").doc(uid), {
          userId: uid,
          role: uid === callerUid ? "ADMIN" : "MEMBER",
          joinedAt: now,
          lastDeliveredAt: EPOCH,
          lastReadAt: EPOCH,
          updatedAt: now,
        } satisfies ConversationMemberDocument);
        transaction.create(inboxRef(uid, conversationRef.id), baseInbox(conversationRef.id, conversation, title, now));
      });
    });
    return { conversationId: conversationRef.id };
  },
);

async function applyMessageFanout(
  eventId: string,
  conversationId: string,
  messageId: string,
  message: MessageDocument,
): Promise<void> {
  const eventRef = db.collection("functionEvents")
    .doc(idempotencyKey("message-created", eventId));
  const conversationRef = db.collection("conversations").doc(conversationId);
  const messageRef = conversationRef.collection("messages").doc(messageId);

  await db.runTransaction(async (transaction): Promise<void> => {
    const eventSnapshot = await transaction.get(eventRef);
    if (eventSnapshot.exists) {
      return;
    }

    const [conversationSnapshot, currentMessageSnapshot] = await Promise.all([
      transaction.get(conversationRef),
      transaction.get(messageRef),
    ]);
    const conversation = assertConversation(conversationSnapshot.data(), conversationId);
    if (!currentMessageSnapshot.exists) {
      throw new Error("Message " + messageId + " no longer exists.");
    }
    const effects = createdMessageEffects(message, currentMessageSnapshot.data());
    if (!conversation.memberIds.includes(message.senderId)) {
      throw new Error("Message sender is not a conversation member.");
    }

    const profileRefs = conversation.memberIds.map(organizationMemberRef);
    const membershipRefs = conversation.memberIds.map(
      (uid) => conversationRef.collection("members").doc(uid),
    );
    const inboxRefs = conversation.memberIds.map((uid) => inboxRef(uid, conversationId));
    const profileSnapshots = await Promise.all(profileRefs.map((ref) => transaction.get(ref)));
    const membershipSnapshots = await Promise.all(
      membershipRefs.map((ref) => transaction.get(ref)),
    );
    const inboxSnapshots = await Promise.all(inboxRefs.map((ref) => transaction.get(ref)));
    const profiles = new Map<string, OrganizationMemberDocument>();
    profileSnapshots.forEach((snapshot, index) => {
      if (snapshot.exists) {
        profiles.set(
          conversation.memberIds[index],
          snapshot.data() as OrganizationMemberDocument,
        );
      }
    });
    if (profiles.get(message.senderId)?.active !== true) {
      throw new Error("Message sender is not an active employee.");
    }

    const now = Timestamp.now();
    conversation.memberIds.forEach((uid, index) => {
      if (profiles.get(uid)?.active !== true) {
        return;
      }
      const existing = inboxSnapshots[index].data() as Partial<InboxDocument> | undefined;
      const lastReadAt = membershipSnapshots[index].get("lastReadAt");
      const alreadyRead = hasReadThrough(
        lastReadAt instanceof Timestamp ? lastReadAt.toMillis() : null,
        message.serverCreatedAt.toMillis(),
      );
      const unreadCount = nextUnreadCount(
        existing?.unreadCount,
        uid === message.senderId,
        alreadyRead,
      );
      const replaceSummary = shouldReplaceMessageSummary(
        existing?.lastMessageAt,
        existing?.lastMessageId,
        message.serverCreatedAt,
        messageId,
      );
      const inbox: DocumentData = {
        conversationId,
        orgId: ORG_ID,
        conversationType: conversation.type,
        title: profileTitle(conversation, uid, profiles),
        unreadCount,
        muted: existing?.muted === true,
        pinned: existing?.pinned === true,
        archived: existing?.archived === true,
        updatedAt: now,
      };
      if (replaceSummary) {
        Object.assign(inbox, {
          lastMessageId: messageId,
          lastMessagePreview: effects.preview,
          lastMessageSenderId: message.senderId,
          lastMessageAt: message.serverCreatedAt,
        });
      }
      transaction.set(inboxRefs[index], inbox, { merge: true });
    });
    if (shouldReplaceMessageSummary(
      conversation.updatedAt,
      null,
      message.serverCreatedAt,
      messageId,
    )) {
      transaction.update(conversationRef, { updatedAt: message.serverCreatedAt });
    }
    const ledger: FunctionEventDocument = {
      eventId,
      eventType: "MESSAGE_CREATED",
      conversationId,
      messageId,
      fanoutCompletedAt: now,
      notificationAttemptedAt: null,
      notificationCompletedAt: null,
      notificationLeaseUntil: EPOCH,
      completedDevicePaths: [],
      expireAt: Timestamp.fromMillis(now.toMillis() + LEDGER_TTL_MILLIS),
    };
    transaction.create(eventRef, ledger);
  });
}

type NotificationClaim = "CLAIMED" | "BUSY" | "COMPLETED";

async function claimNotificationDelivery(
  eventId: string,
): Promise<NotificationClaim> {
  const eventRef = db.collection("functionEvents")
    .doc(idempotencyKey("message-created", eventId));
  return db.runTransaction(async (transaction): Promise<NotificationClaim> => {
    const snapshot = await transaction.get(eventRef);
    if (!snapshot.exists) {
      throw new Error("Message fan-out ledger is missing.");
    }
    const ledger = snapshot.data() as FunctionEventDocument;
    if (ledger.notificationCompletedAt != null) {
      return "COMPLETED";
    }
    const now = Timestamp.now();
    if (ledger.notificationLeaseUntil instanceof Timestamp &&
        ledger.notificationLeaseUntil.toMillis() > now.toMillis()) {
      return "BUSY";
    }
    transaction.update(eventRef, {
      notificationAttemptedAt: now,
      notificationLeaseUntil: Timestamp.fromMillis(now.toMillis() + 90_000),
    });
    return "CLAIMED";
  });
}

async function releaseNotificationClaim(eventId: string): Promise<void> {
  await db.collection("functionEvents")
    .doc(idempotencyKey("message-created", eventId))
    .set({ notificationLeaseUntil: EPOCH }, { merge: true });
}

async function notificationCandidates(
  eventRef: DocumentReference,
  conversationId: string,
  messageId: string,
  message: MessageDocument,
): Promise<NotificationCandidate[]> {
  const [
    eventSnapshot,
    conversationSnapshot,
    senderSnapshot,
    currentMessageSnapshot,
  ] = await Promise.all([
    eventRef.get(),
    db.collection("conversations").doc(conversationId).get(),
    organizationMemberRef(message.senderId).get(),
    db.collection("conversations").doc(conversationId)
      .collection("messages").doc(messageId).get(),
  ]);
  const ledger = eventSnapshot.data() as FunctionEventDocument | undefined;
  if (ledger == null || ledger.notificationCompletedAt != null) {
    return [];
  }
  if (!currentMessageSnapshot.exists) {
    return [];
  }
  const effects = createdMessageEffects(message, currentMessageSnapshot.data());
  if (!effects.notify) return [];
  const completed = new Set(ledger.completedDevicePaths ?? []);
  const conversation = assertConversation(conversationSnapshot.data(), conversationId);
  const sender = senderSnapshot.exists
    ? senderSnapshot.data() as OrganizationMemberDocument
    : null;
  const senderName = displayName(sender?.displayName);
  const recipients = recipientIds(conversation.memberIds, message.senderId);
  const candidates: NotificationCandidate[] = [];

  await Promise.all(recipients.map(async (uid) => {
    const [profileSnapshot, inboxSnapshot, devicesSnapshot] = await Promise.all([
      organizationMemberRef(uid).get(),
      inboxRef(uid, conversationId).get(),
      privateMemberRef(uid).collection("devices").where("enabled", "==", true).get(),
    ]);
    const profile = activeMember(profileSnapshot);
    const inbox = inboxSnapshot.data() as Partial<InboxDocument> | undefined;
    if (!canSendNotification(profile, inbox)) {
      return;
    }
    const title = notificationTitle(conversation.type, conversation.title, senderName);
    for (const deviceSnapshot of devicesSnapshot.docs) {
      const devicePath = deviceSnapshot.ref.path;
      const device = deviceSnapshot.data();
      if (completed.has(devicePath) || typeof device.token !== "string" ||
          device.token.length === 0 || device.platform !== "ANDROID") {
        continue;
      }
      candidates.push({
        devicePath,
        token: device.token,
        message: {
          token: device.token,
          data: {
            type: "chat_message",
            orgId: ORG_ID,
            conversationId,
            messageId,
            senderId: message.senderId,
            recipientId: uid,
            title,
            preview: effects.preview,
          },
          android: {
            priority: "high",
          },
        },
      });
    }
  }));
  return candidates;
}

async function deliverNotifications(
  eventId: string,
  conversationId: string,
  messageId: string,
  message: MessageDocument,
): Promise<void> {
  const eventRef = db.collection("functionEvents")
    .doc(idempotencyKey("message-created", eventId));
  const candidates = await notificationCandidates(
    eventRef,
    conversationId,
    messageId,
    message,
  );
  if (candidates.length === 0) {
    await eventRef.set({
      notificationAttemptedAt: Timestamp.now(),
      notificationCompletedAt: Timestamp.now(),
      notificationLeaseUntil: EPOCH,
    }, { merge: true });
    return;
  }

  const completedDevicePaths: string[] = [];
  const invalidDevicePaths: string[] = [];
  let transientFailureCount = 0;
  for (let offset = 0; offset < candidates.length; offset += 500) {
    const chunk = candidates.slice(offset, offset + 500);
    const response = await getMessaging().sendEach(chunk.map((candidate) => candidate.message));
    response.responses.forEach((item, index) => {
      const candidate = chunk[index];
      if (item.success) {
        completedDevicePaths.push(candidate.devicePath);
        return;
      }
      const code = item.error?.code;
      if (isPermanentFcmTokenError(code)) {
        invalidDevicePaths.push(candidate.devicePath);
      } else {
        transientFailureCount += 1;
        logger.warn("Transient FCM delivery failure", {
          conversationId,
          messageId,
          code: code ?? "unknown",
        });
      }
    });
  }

  const cleanupResults = await Promise.allSettled(
    invalidDevicePaths.map((path) => db.doc(path).delete()),
  );
  cleanupResults.forEach((result, index) => {
    if (result.status === "fulfilled") {
      completedDevicePaths.push(invalidDevicePaths[index]);
    } else {
      transientFailureCount += 1;
      logger.warn("Invalid FCM token cleanup failed", {
        conversationId,
        messageId,
      });
    }
  });
  const ledgerUpdate: DocumentData = {
    notificationAttemptedAt: Timestamp.now(),
    notificationLeaseUntil: EPOCH,
  };
  if (completedDevicePaths.length > 0) {
    ledgerUpdate.completedDevicePaths = FieldValue.arrayUnion(...completedDevicePaths);
  }
  if (transientFailureCount === 0) {
    ledgerUpdate.notificationCompletedAt = Timestamp.now();
  }
  await eventRef.set(ledgerUpdate, { merge: true });
  if (transientFailureCount > 0) {
    throw new Error("FCM delivery had " + transientFailureCount + " transient failure(s).");
  }
}

export const onMessageCreated = onDocumentCreated(
  {
    document: MESSAGE_DOCUMENT,
    retry: true,
  },
  async (event): Promise<void> => {
    if (event.data == null) {
      return;
    }
    const conversationId = event.params.conversationId;
    const messageId = event.params.messageId;
    const message = assertMessage(event.data.data(), messageId);
    await applyMessageFanout(
      event.id,
      conversationId,
      messageId,
      message,
    );
    const claim = await claimNotificationDelivery(event.id);
    if (claim === "COMPLETED") {
      return;
    }
    if (claim === "BUSY") {
      throw new Error("Notification delivery is already claimed; retrying later.");
    }
    try {
      await deliverNotifications(event.id, conversationId, messageId, message);
    } catch (error) {
      await releaseNotificationClaim(event.id);
      throw error;
    }
  },
);

async function applyDeletionFanout(
  eventId: string,
  conversationId: string,
  messageId: string,
): Promise<void> {
  const eventRef = db.collection("functionEvents")
    .doc(idempotencyKey("message-deleted", eventId));
  const conversationRef = db.collection("conversations").doc(conversationId);
  await db.runTransaction(async (transaction): Promise<void> => {
    const eventSnapshot = await transaction.get(eventRef);
    if (eventSnapshot.exists) {
      return;
    }
    const conversationSnapshot = await transaction.get(conversationRef);
    const conversation = assertConversation(conversationSnapshot.data(), conversationId);
    const inboxRefs = conversation.memberIds.map((uid) => inboxRef(uid, conversationId));
    const inboxSnapshots = await Promise.all(inboxRefs.map((ref) => transaction.get(ref)));
    const now = Timestamp.now();
    inboxSnapshots.forEach((snapshot, index) => {
      if (snapshot.data()?.lastMessageId === messageId) {
        transaction.set(inboxRefs[index], {
          lastMessagePreview: DELETED_MESSAGE_PREVIEW,
          updatedAt: now,
        }, { merge: true });
      }
    });
    const ledger: FunctionEventDocument = {
      eventId,
      eventType: "MESSAGE_DELETED",
      conversationId,
      messageId,
      fanoutCompletedAt: now,
      notificationAttemptedAt: now,
      notificationCompletedAt: now,
      notificationLeaseUntil: EPOCH,
      completedDevicePaths: [],
      expireAt: Timestamp.fromMillis(now.toMillis() + LEDGER_TTL_MILLIS),
    };
    transaction.create(eventRef, ledger);
  });
}

export const onMessageDeleted = onDocumentUpdated(
  {
    document: MESSAGE_DOCUMENT,
    retry: true,
  },
  async (event): Promise<void> => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (before == null || after == null ||
        !becameDeleted(before.deletedAt, after.deletedAt)) {
      return;
    }
    await applyDeletionFanout(
      event.id,
      event.params.conversationId,
      event.params.messageId,
    );
  },
);

export const onConversationReceiptUpdated = onDocumentUpdated(
  {
    document: CONVERSATION_MEMBER_DOCUMENT,
    retry: true,
  },
  async (event): Promise<void> => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (before == null || after == null ||
        !(before.lastReadAt instanceof Timestamp) ||
        !(after.lastReadAt instanceof Timestamp) ||
        after.lastReadAt.toMillis() <= before.lastReadAt.toMillis()) {
      return;
    }
    const uid = event.params.uid;
    const conversationId = event.params.conversationId;
    const memberInboxRef = inboxRef(uid, conversationId);
    await db.runTransaction(async (transaction): Promise<void> => {
      const inboxSnapshot = await transaction.get(memberInboxRef);
      if (!inboxSnapshot.exists) {
        return;
      }
      const lastMessageAt = inboxSnapshot.get("lastMessageAt");
      const currentUnreadCount = inboxSnapshot.get("unreadCount");
      const updatedUnreadCount = unreadCountAfterReceipt(
        currentUnreadCount,
        lastMessageAt instanceof Timestamp ? lastMessageAt.toMillis() : null,
        after.lastReadAt.toMillis(),
      );
      // Never clear a newer message that raced this receipt trigger. The next
      // foreground watermark will cover it and safely reset the full count.
      if (updatedUnreadCount === currentUnreadCount) {
        return;
      }
      transaction.update(memberInboxRef, {
        unreadCount: updatedUnreadCount,
        updatedAt: Timestamp.now(),
      });
    });
  },
);
