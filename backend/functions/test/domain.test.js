"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  DELETED_MESSAGE_PREVIEW,
  becameDeleted,
  canSendNotification,
  createdMessageEffects,
  directConversationId,
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
} = require("../src/domain");

test("direct conversation ID is stable regardless of member order", () => {
  const first = directConversationId("innovfix", "alice", "bob");
  const second = directConversationId("innovfix", "bob", "alice");
  assert.equal(first, second);
  assert.match(first, /^direct_[a-f0-9]{40}$/);
});

test("direct conversation ID is scoped to the organization", () => {
  assert.notEqual(
    directConversationId("innovfix", "alice", "bob"),
    directConversationId("another-org", "alice", "bob"),
  );
});

test("idempotency keys are stable, type scoped, and safe document IDs", () => {
  const key = idempotencyKey("message-created", "projects/a/events/123");
  assert.match(key, /^[a-f0-9]{64}$/);
  assert.equal(key, idempotencyKey("message-created", "projects/a/events/123"));
  assert.notEqual(key, idempotencyKey("message-deleted", "projects/a/events/123"));
});

test("message previews collapse whitespace and have a safe maximum", () => {
  assert.equal(textPreview(" hello\n  team "), "hello team");
  assert.equal(textPreview("x".repeat(200)).length, 160);
  assert.equal(messagePreview({ text: "secret", deletedAt: new Date() }), DELETED_MESSAGE_PREVIEW);
});

test("attachment previews match the chat inbox", () => {
  assert.equal(messagePreview({ kind: "IMAGE", text: "Photo" }), "Photo");
  assert.equal(messagePreview({ kind: "DOCUMENT", fileName: "brief.pdf", text: "brief.pdf" }), "brief.pdf");
  assert.equal(messagePreview({ kind: "AUDIO", text: "Voice message" }), "Voice message");
});

test("group notifications include sender and group while direct does not", () => {
  assert.equal(notificationTitle("GROUP", "Product", "Mia"), "Mia · Product");
  assert.equal(notificationTitle("DIRECT", null, "Mia"), "Mia");
});

test("deletion transition only fires once", () => {
  assert.equal(becameDeleted(null, new Date()), true);
  assert.equal(becameDeleted(undefined, null), false);
  assert.equal(becameDeleted(new Date(1), new Date(2)), false);
});

test("delete-before-create event order keeps a tombstone and suppresses notification", () => {
  const created = { text: "confidential", deletedAt: null };
  const current = { text: "", deletedAt: new Date(2_000) };
  assert.deepEqual(createdMessageEffects(created, current), {
    preview: DELETED_MESSAGE_PREVIEW,
    notify: false,
  });
});

test("create-before-delete event order publishes once then converges to tombstone", () => {
  const created = { text: "hello team", deletedAt: null };
  const initialEffects = createdMessageEffects(created, created);
  assert.deepEqual(initialEffects, { preview: "hello team", notify: true });
  const deleted = { text: "", deletedAt: new Date(2_000) };
  assert.equal(becameDeleted(created.deletedAt, deleted.deletedAt), true);
  assert.equal(messagePreview(deleted), DELETED_MESSAGE_PREVIEW);
});

test("recipient list excludes sender, duplicates, and invalid values", () => {
  assert.deepEqual(recipientIds(["a", "b", "b", "", 7], "a"), ["b"]);
});

test("only permanent FCM token failures request token cleanup", () => {
  assert.equal(isPermanentFcmTokenError("messaging/registration-token-not-registered"), true);
  assert.equal(isPermanentFcmTokenError("messaging/invalid-registration-token"), true);
  assert.equal(isPermanentFcmTokenError("messaging/internal-error"), false);
});

test("unread fan-out increments recipients once and preserves sender count", () => {
  assert.equal(nextUnreadCount(4, false), 5);
  assert.equal(nextUnreadCount(4, true), 4);
  assert.equal(nextUnreadCount(-1, false), 1);
  assert.equal(nextUnreadCount("4", false), 1);
});

test("receipt watermark suppresses a delayed unread fan-out", () => {
  assert.equal(hasReadThrough(1_000, 1_000), true);
  assert.equal(hasReadThrough(1_001, 1_000), true);
  assert.equal(hasReadThrough(999, 1_000), false);
  assert.equal(hasReadThrough(null, 1_000), false);
  assert.equal(nextUnreadCount(4, false, true), 4);
});

test("message and receipt commit order converges without resurrecting unread", () => {
  // If fan-out commits first, the receipt trigger clears the increment.
  const messageThenReceipt = unreadCountAfterReceipt(
    nextUnreadCount(0, false, false),
    2_000,
    2_000,
  );
  // If the receipt commits first, the retried fan-out reads its watermark and skips the increment.
  const receiptThenMessage = nextUnreadCount(
    unreadCountAfterReceipt(0, 1_000, 2_000),
    false,
    hasReadThrough(2_000, 2_000),
  );
  assert.equal(messageThenReceipt, 0);
  assert.equal(receiptThenMessage, 0);
  assert.equal(unreadCountAfterReceipt(3, 2_001, 2_000), 3);
  assert.equal(nextUnreadCount(0, false, hasReadThrough(1_999, 2_000)), 1);
});

test("older message events cannot replace a newer inbox summary", () => {
  assert.equal(shouldReplaceMessageSummary(
    { seconds: 20, nanoseconds: 0 },
    "01900000-0000-7000-8000-000000000002",
    { seconds: 19, nanoseconds: 999_999_999 },
    "01900000-0000-7000-8000-000000000003",
  ), false);
});

test("newer message events replace an older inbox summary", () => {
  assert.equal(shouldReplaceMessageSummary(
    { seconds: 20, nanoseconds: 999_999_998 },
    "01900000-0000-7000-8000-000000000003",
    { seconds: 20, nanoseconds: 999_999_999 },
    "01900000-0000-7000-8000-000000000001",
  ), true);
});

test("equal server timestamps use UUIDv7 message IDs as a stable tie-breaker", () => {
  const timestamp = { seconds: 20, nanoseconds: 123 };
  const lower = "01900000-0000-7000-8000-000000000001";
  const higher = "01900000-0000-7000-8000-000000000002";
  assert.equal(shouldReplaceMessageSummary(timestamp, higher, timestamp, lower), false);
  assert.equal(shouldReplaceMessageSummary(timestamp, lower, timestamp, higher), true);
  assert.equal(shouldReplaceMessageSummary(timestamp, higher, timestamp, higher), true);
});

test("notification eligibility respects active, global, and conversation mute", () => {
  assert.equal(canSendNotification(
    { active: true, notificationPreference: "ALL" },
    { muted: false },
  ), true);
  assert.equal(canSendNotification(
    { active: false, notificationPreference: "ALL" },
    { muted: false },
  ), false);
  assert.equal(canSendNotification(
    { active: true, notificationPreference: "NONE" },
    { muted: false },
  ), false);
  assert.equal(canSendNotification(
    { active: true, notificationPreference: "ALL" },
    { muted: true },
  ), false);
});
