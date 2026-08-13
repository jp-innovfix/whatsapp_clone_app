"use strict";

const ORGANIZATION_ROLES = new Set(["ADMIN", "MEMBER"]);
const CONVERSATION_ROLES = new Set(["ADMIN", "MEMBER"]);

/**
 * @param {unknown} value
 * @param {string} label
 * @returns {string}
 */
function identifier(value, label) {
  if (typeof value !== "string") {
    throw new Error(label + " is required.");
  }
  const result = value.trim();
  if (result.length === 0 || result.length > 128 || result.includes("/")) {
    throw new Error(label + " must be 1-128 characters and cannot contain '/'.");
  }
  return result;
}

/**
 * @param {unknown} value
 * @returns {string}
 */
function emailAddress(value) {
  if (typeof value !== "string") {
    throw new Error("email is required.");
  }
  const result = value.trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(result) || result.length > 254) {
    throw new Error("email must be a valid email address.");
  }
  return result;
}

/**
 * @param {unknown} value
 * @param {string} label
 * @param {number} maximum
 * @returns {string}
 */
function boundedText(value, label, maximum) {
  if (typeof value !== "string") {
    throw new Error(label + " is required.");
  }
  const result = value.trim().replace(/\s+/g, " ");
  if (result.length === 0 || result.length > maximum) {
    throw new Error(label + " must be 1-" + maximum + " characters.");
  }
  return result;
}

/**
 * @param {unknown} value
 * @returns {"ADMIN" | "MEMBER"}
 */
function organizationRole(value) {
  const result = String(value ?? "MEMBER").trim().toUpperCase();
  if (!ORGANIZATION_ROLES.has(result)) {
    throw new Error("role must be ADMIN or MEMBER.");
  }
  return /** @type {"ADMIN" | "MEMBER"} */ (result);
}

/**
 * @param {unknown} value
 * @returns {"ADMIN" | "MEMBER"}
 */
function conversationRole(value) {
  const result = String(value ?? "MEMBER").trim().toUpperCase();
  if (!CONVERSATION_ROLES.has(result)) {
    throw new Error("group role must be ADMIN or MEMBER.");
  }
  return /** @type {"ADMIN" | "MEMBER"} */ (result);
}

/**
 * @param {unknown} value
 * @param {string} label
 * @returns {string[]}
 */
function identifierList(value, label) {
  if (typeof value !== "string") {
    throw new Error(label + " is required.");
  }
  const result = [...new Set(value.split(",").map((item) => identifier(item, label)))].sort();
  if (result.length === 0 || result.length > 100) {
    throw new Error(label + " must contain 1-100 comma-separated user IDs.");
  }
  return result;
}

/**
 * @param {string[]} argv
 * @returns {{command: string, options: Record<string, string | boolean>}}
 */
function parseCommandLine(argv) {
  if (argv.length === 0 || argv[0].startsWith("--")) {
    throw new Error("A command is required. Run with help for usage.");
  }
  const command = argv[0];
  /** @type {Record<string, string | boolean>} */
  const options = {};
  for (let index = 1; index < argv.length; index += 1) {
    const current = argv[index];
    if (!current.startsWith("--") || current.length <= 2) {
      throw new Error("Unexpected argument: " + current);
    }
    const name = current.slice(2);
    if (Object.prototype.hasOwnProperty.call(options, name)) {
      throw new Error("Duplicate option: --" + name);
    }
    const next = argv[index + 1];
    if (next == null || next.startsWith("--")) {
      options[name] = true;
    } else {
      options[name] = next;
      index += 1;
    }
  }
  return { command, options };
}

/**
 * @param {Record<string, string | boolean>} options
 * @param {string} name
 * @param {boolean} [required]
 * @returns {string | undefined}
 */
function option(options, name, required = true) {
  const value = options[name];
  if (typeof value === "boolean") {
    throw new Error("--" + name + " requires a value.");
  }
  if (value == null && required) {
    throw new Error("--" + name + " is required.");
  }
  return value;
}

module.exports = {
  boundedText,
  conversationRole,
  emailAddress,
  identifier,
  identifierList,
  option,
  organizationRole,
  parseCommandLine,
};
