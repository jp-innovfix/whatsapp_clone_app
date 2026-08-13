"use strict";

const { createHash } = require("node:crypto");

const DIRECT = "DIRECT";
const GROUP = "GROUP";
const DELETED_MESSAGE_PREVIEW = "This message was deleted";

/**
 * @param {string} orgId
 * @param {string} firstUid
 * @param {string} secondUid
 * @returns {string}
 */
function directConversationId(orgId, firstUid, secondUid) {
  const pair = [firstUid, secondUid].sort();
  const digest = createHash("sha256")
    .update(orgId + ":" + pair[0] + ":" + pair[1], "utf8")
    .digest("hex")
    .slice(0, 40);
  return "direct_" + digest;
}

/**
 * @param {string} eventType
 * @param {string} eventId
 * @returns {string}
 */
function idempotencyKey(eventType, eventId) {
  return createHash("sha256")
    .update(eventType + ":" + eventId, "utf8")
    .digest("hex");
}

/**
 * @param {unknown} value
 * @returns {string}
 */
function displayName(value) {
  return typeof value === "string" && value.trim().length > 0
    ? value.trim()
    : "Employee";
}

/**
 * @param {unknown} value
 * @returns {string}
 */
function textPreview(value) {
  if (typeof value !== "string") {
    return "";
  }
  return value.trim().replace(/\s+/g, " ").slice(0, 160);
}

/**
 * @param {{deletedAt?: unknown, text?: unknown, kind?: unknown, fileName?: unknown}} message
 * @returns {string}
 */
function messagePreview(message) {
  if (message.deletedAt != null) return DELETED_MESSAGE_PREVIEW;
  switch (message.kind) {
    case "IMAGE": return "Photo";
    case "DOCUMENT": return textPreview(message.fileName) || "Document";
    case "AUDIO": return "Voice message";
    default: return textPreview(message.text);
  }
}

/**
 * Resolve create-trigger effects against the message's current document so a
 * delayed create event cannot resurrect content after a delete event.
 * @param {{deletedAt?: unknown, text?: unknown, kind?: unknown, fileName?: unknown}} createdMessage
 * @param {{deletedAt?: unknown} | null | undefined} currentMessage
 * @returns {{preview: string, notify: boolean}}
 */
function createdMessageEffects(createdMessage, currentMessage) {
  const deleted = currentMessage?.deletedAt != null;
  return {
    preview: deleted ? DELETED_MESSAGE_PREVIEW : messagePreview(createdMessage),
    notify: !deleted,
  };
}

/**
 * @param {unknown} value
 * @returns {{seconds: number, nanoseconds: number} | null}
 */
function timestampParts(value) {
  if (typeof value !== "object" || value == null) {
    return null;
  }
  const record = /** @type {Record<string, unknown>} */ (value);
  const { seconds, nanoseconds } = record;
  if (typeof seconds !== "number" || typeof nanoseconds !== "number" ||
      !Number.isSafeInteger(seconds) || !Number.isSafeInteger(nanoseconds) ||
      nanoseconds < 0 || nanoseconds >= 1_000_000_000) {
    return null;
  }
  return { seconds, nanoseconds };
}

/**
 * Firestore create events are not ordered. Keep an inbox summary monotonic by
 * server time, with the sortable UUIDv7 document ID as a deterministic tie-breaker.
 * @param {unknown} existingAt
 * @param {unknown} existingMessageId
 * @param {unknown} incomingAt
 * @param {unknown} incomingMessageId
 * @returns {boolean}
 */
function shouldReplaceMessageSummary(
  existingAt,
  existingMessageId,
  incomingAt,
  incomingMessageId,
) {
  const incoming = timestampParts(incomingAt);
  if (incoming == null) return false;
  const existing = timestampParts(existingAt);
  if (existing == null) return true;
  if (incoming.seconds !== existing.seconds) {
    return incoming.seconds > existing.seconds;
  }
  if (incoming.nanoseconds !== existing.nanoseconds) {
    return incoming.nanoseconds > existing.nanoseconds;
  }
  if (typeof incomingMessageId !== "string" || incomingMessageId.length === 0) {
    return false;
  }
  return typeof existingMessageId !== "string" ||
    existingMessageId.length === 0 || incomingMessageId >= existingMessageId;
}

/**
 * @param {string} conversationType
 * @param {string | null | undefined} groupTitle
 * @param {string} senderName
 * @returns {string}
 */
function notificationTitle(conversationType, groupTitle, senderName) {
  const sender = displayName(senderName);
  if (conversationType !== GROUP) {
    return sender;
  }
  const title = displayName(groupTitle);
  return sender + " · " + title;
}

/**
 * @param {unknown} code
 * @returns {boolean}
 */
function isPermanentFcmTokenError(code) {
  return code === "messaging/invalid-registration-token" ||
    code === "messaging/registration-token-not-registered";
}

/**
 * @param {unknown} before
 * @param {unknown} after
 * @returns {boolean}
 */
function becameDeleted(before, after) {
  return before == null && after != null;
}

/**
 * @param {unknown} memberIds
 * @param {string} senderId
 * @returns {string[]}
 */
function recipientIds(memberIds, senderId) {
  if (!Array.isArray(memberIds)) {
    return [];
  }
  return [...new Set(memberIds)]
    .filter((uid) => typeof uid === "string" && uid.length > 0 && uid !== senderId);
}

/**
 * @param {unknown} current
 * @param {boolean} sentByViewer
 * @param {boolean} [alreadyRead]
 * @returns {number}
 */
function nextUnreadCount(current, sentByViewer, alreadyRead = false) {
  const normalized = typeof current === "number" &&
    Number.isSafeInteger(current) && current >= 0 ? current : 0;
  return sentByViewer || alreadyRead ? normalized : normalized + 1;
}

/**
 * @param {unknown} lastReadAtMillis
 * @param {unknown} messageAtMillis
 * @returns {boolean}
 */
function hasReadThrough(lastReadAtMillis, messageAtMillis) {
  return typeof lastReadAtMillis === "number" &&
    Number.isFinite(lastReadAtMillis) &&
    typeof messageAtMillis === "number" &&
    Number.isFinite(messageAtMillis) &&
    lastReadAtMillis >= messageAtMillis;
}

/**
 * @param {unknown} current
 * @param {unknown} lastMessageAtMillis
 * @param {unknown} lastReadAtMillis
 * @returns {number}
 */
function unreadCountAfterReceipt(current, lastMessageAtMillis, lastReadAtMillis) {
  const normalized = typeof current === "number" &&
    Number.isSafeInteger(current) && current >= 0 ? current : 0;
  if (lastMessageAtMillis == null || hasReadThrough(lastReadAtMillis, lastMessageAtMillis)) {
    return 0;
  }
  return normalized;
}

/**
 * @param {{active?: unknown, notificationPreference?: unknown} | null | undefined} profile
 * @param {{muted?: unknown} | null | undefined} inbox
 * @returns {boolean}
 */
function canSendNotification(profile, inbox) {
  return profile?.active === true &&
    profile.notificationPreference !== "NONE" &&
    inbox?.muted !== true;
}

module.exports = {
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
  textPreview,
  unreadCountAfterReceipt,
};
