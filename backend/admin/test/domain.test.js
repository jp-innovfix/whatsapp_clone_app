"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  boundedText,
  conversationRole,
  emailAddress,
  identifier,
  identifierList,
  option,
  organizationRole,
  parseCommandLine,
} = require("../src/domain");

test("command parser supports value and boolean options", () => {
  assert.deepEqual(
    parseCommandLine(["invite", "--email", "test@example.com", "--dry-run"]),
    {
      command: "invite",
      options: { email: "test@example.com", "dry-run": true },
    },
  );
});

test("command parser rejects duplicate options", () => {
  assert.throws(
    () => parseCommandLine(["role", "--uid", "a", "--uid", "b"]),
    /Duplicate option/,
  );
});

test("required option and identifier validation are strict", () => {
  assert.equal(option({ uid: "alice" }, "uid"), "alice");
  assert.throws(() => option({}, "uid"), /required/);
  assert.equal(identifier(" alice ", "uid"), "alice");
  assert.throws(() => identifier("bad/id", "uid"), /cannot contain/);
});

test("emails, titles, and roles normalize to canonical forms", () => {
  assert.equal(emailAddress(" Alice@Example.COM "), "alice@example.com");
  assert.equal(boundedText(" Product   Team ", "title", 80), "Product Team");
  assert.equal(organizationRole("admin"), "ADMIN");
  assert.equal(conversationRole(undefined), "MEMBER");
  assert.throws(() => organizationRole("OWNER"), /ADMIN or MEMBER/);
});

test("identifier lists de-duplicate and sort", () => {
  assert.deepEqual(identifierList("carol,alice,carol,bob", "members"), [
    "alice",
    "bob",
    "carol",
  ]);
});
