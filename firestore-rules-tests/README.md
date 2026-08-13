# Firestore rules tests

These tests run only against the Firebase Firestore emulator. They never use a
deployed Firebase project; the project ID is deliberately prefixed with
`demo-`.

From this directory:

```shell
npm install
npm test
```

If the emulator is already running on `127.0.0.1:8080`, run
`npm run test:rules` instead.

The test command starts the Firestore emulator with the repository's
`firebase.json`, loads `firestore.rules` and `firestore.indexes.json`, runs the
Node test suite, and then shuts the emulator down.

Java is required by the Firestore emulator. No service-account credential is
needed or expected. On this Windows workstation, Android Studio's bundled JBR
is at `C:\Program Files\Android\Android Studio\jbr`; set `JAVA_HOME` to that
directory if `java` is not already on `PATH`.
