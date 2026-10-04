# 1.12.0-dev

- Long-press the agent selector to search and select an available primary agent.
- Configure the app theme and accent, with project and session overrides. Session preferences inherit from the project, then the app. Project lookup supports directory-only sessions.
- Enable or disable automatic scrolling. Disabling it also prevents the initial jump; the manual jump button remains available.
- Show or hide a server password while editing; visibility resets when the dialog opens.
- Browse available agents and subagents in server settings, with hidden agents identified.
- Copy or download a complete session as Markdown, native JSON or HTML.
- View the session file diff, including before and after content.
- Set timeline detail per server or session. Five levels and six activity categories support visibility, grouping and collapse. Messages, attachments and required permission/question interactions remain visible. Grouping preserves activity order.
- Configure completion, permission, question and error notifications separately: enabled, sound, vibration and timing. App, server and session scopes support inheritance, with session mute taking priority.
- Audit library, plugin, SDK and Gradle updates against upstream metadata. See [the dependency audit](../docs/DEPENDENCY_AUDIT_2026-10-04.md).

Appearance, timeline and notification overrides stay on this device. Existing
notification defaults are preserved until an explicit override is saved. Custom
sounds are selected in Android channel settings; configurations using the same
category/sound/vibration channel share that Android setting.

Native JSON downloads preserve the complete export using bounded streaming buffers.
Clipboard copies are limited to 64 Ki characters. Markdown/HTML conversion fails
explicitly for a JSON literal above 64 Ki characters, nesting above 64 or field paths
above 8 Ki characters; use native JSON for those sessions. Readable formats omit
inline binary attachment payloads and file attachment URLs, and retain tool arguments.

No dependency upgrades were applied as part of the update audit. New releases listed
there require a compatible toolchain and regression validation before migration.

Validation: 341 unit tests passed with no failures or errors. Debug and instrumentation
APKs built successfully; Android lint passed with zero errors (216 warnings); APK
signature verified. Physical interface checks await the device endpoint from the user.
