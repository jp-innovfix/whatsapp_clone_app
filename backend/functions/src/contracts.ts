import type { Timestamp } from "firebase-admin/firestore";

export const ORG_ID = "innovfix";
export const REGION = "asia-south1";
export const SCHEMA_VERSION = 1;
export const MAX_MESSAGE_LENGTH = 4_000;

export type ConversationType = "DIRECT" | "GROUP";
export type OrganizationRole = "MEMBER" | "ADMIN";
export type ConversationRole = "ADMIN" | "MEMBER";
export type NotificationPreference = "ALL" | "NONE";

export interface OrganizationMemberDocument {
  email: string;
  displayName: string;
  role: OrganizationRole;
  active: boolean;
  avatarKey: string | null;
  notificationPreference: NotificationPreference;
  lastSeenAt: Timestamp | null;
  activeUntil: Timestamp | null;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface DeviceDocument {
  installationId: string;
  token: string;
  platform: "ANDROID";
  enabled: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface ConversationDocument {
  orgId: string;
  type: ConversationType;
  memberIds: string[];
  title: string | null;
  createdBy: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
  schemaVersion: number;
}

export interface ConversationMemberDocument {
  userId: string;
  role: ConversationRole;
  joinedAt: Timestamp;
  lastDeliveredAt: Timestamp;
  lastReadAt: Timestamp;
  updatedAt: Timestamp;
}

export interface MessageDocument {
  senderId: string;
  text: string;
  clientCreatedAt: Timestamp;
  serverCreatedAt: Timestamp;
  replyToMessageId: string | null;
  deletedAt: Timestamp | null;
  schemaVersion: number;
  kind?: "TEXT" | "IMAGE" | "DOCUMENT" | "AUDIO";
  storagePath?: string | null;
  fileName?: string | null;
  mimeType?: string | null;
  sizeBytes?: number | null;
  durationMillis?: number | null;
}

export interface ReactionDocument {
  messageId: string;
  userId: string;
  emoji: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface TypingDocument {
  userId: string;
  isTyping: boolean;
  expiresAt: Timestamp;
  updatedAt: Timestamp;
}

export interface InboxDocument {
  conversationId: string;
  orgId: string;
  conversationType: ConversationType;
  title: string;
  lastMessageId: string | null;
  lastMessagePreview: string;
  lastMessageSenderId: string | null;
  lastMessageAt: Timestamp | null;
  unreadCount: number;
  muted: boolean;
  pinned: boolean;
  archived: boolean;
  updatedAt: Timestamp;
}

export interface DirectConversationRequest {
  targetUserId: string;
}

export interface DirectConversationResponse {
  conversationId: string;
  created: boolean;
}

export interface GroupConversationResponse {
  conversationId: string;
}

export interface AnonymousBootstrapResponse {
  userId: string;
  demoEmployeeId: string;
}
