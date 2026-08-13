# Project UI Verification Rules

These rules apply to every UI implementation or refinement in this project.

1. Treat the supplied WhatsApp Business screenshots as the visual source of truth.
2. Before changing a state, inspect its reference at original resolution and convert pixel measurements using the target Samsung display metrics (1080x2340 at 450 dpi, unless the connected device reports otherwise).
3. For each target state, temporarily make that exact state open automatically without user interaction. This may include a temporary start destination, seeded selected item, seeded playback progress, opened sheet/menu, or focused composer.
4. Build, install, cold-launch on the connected Samsung, capture a device screenshot, and compare it with the reference. Check geometry, alignment, typography, colors, opacity, shadows, radii, icon/emoji size, clipping, system insets, and interactive-state feedback.
5. Iterate with the same automatic capture loop until the state is visually aligned and stable.
6. Remove every temporary auto-open/forced-state hook after verification. The delivered build must retain the normal user interaction flow.
7. Verify the normal build with unit tests, lint, APK assembly, installation, cold launch, and a clean AndroidRuntime crash log.
8. For interactive media, verify both appearance and behavior. Voice messages must support play/pause/resume, progress, completion, one active clip at a time, recorded-file playback, seeded-demo playback, and speed changes where shown by the reference.
9. Never use negative Compose padding. Use `offset`, translation, or parent layout positioning for deliberate overlap.
10. Preserve unrelated user changes and do not leave verification-only code in the final app.
