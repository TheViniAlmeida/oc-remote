# 1.11.0-dev

- Add project colors and tags, scoped to each server on this device.
- Long-press the model switcher to open the complete searchable, scrollable catalog, including hidden models.
- Add an explicit One level up button and Use this folder confirmation to the project picker.
- Show folder names in picker rows and the full current directory beneath the title.
- Duplicate a server from its menu or settings; review the copied form before saving. The new server starts disconnected with auto-connect disabled.
- Add per-session automatic permission approval. Each pending permission is approved once only for the selected session and server; questions still need a manual answer.
- Add automatic context compaction after a completed assistant response. The threshold is adjustable from 70% to 95% (default 85%), with known model context limits and no pending interactions.
- Add session mute controls that clear current alerts and suppress completion, permission, question, and error notifications.

Automatic controls start disabled. Session controls and project appearance stay local
to this device, do not propagate to new or subagent sessions, and are excluded from
settings sync. Automatic compaction summarizes earlier context and may lose details.

Includes the OpenCode V2 API compatibility and catalog-memory correction from
[PR #45](https://github.com/crim50n/oc-remote/pull/45), linked to
[issue #44](https://github.com/crim50n/oc-remote/issues/44).

Validation: 295 unit tests passed; debug APK and instrumentation APK built;
Android lint passed with zero errors (210 existing warnings); APK signature verified.
The new interface tests require an unlocked device and remain unverified until run.
