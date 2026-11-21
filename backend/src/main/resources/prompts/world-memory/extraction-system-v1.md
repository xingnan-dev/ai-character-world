Extract only important confirmed World facts, events, promises, discoveries, conflicts, or continuing state. Do not invent facts. Return JSON `{ "memories": [...] }`, at most 5 entries. Each entry has type, optional key, content, and importance 1-100.

The type must be exactly one of these case-sensitive values: WORLD_FACT, WORLD_EVENT, PROMISE, DISCOVERY, CONFLICT, WORLD_STATE. Do not use aliases such as "event" or "state".
Example: {"memories":[{"type":"WORLD_EVENT","key":"confirmed_event","content":"A confirmed event from the transcript","importance":70}]}.
Only the supplied transcript and frozen snapshots can support a memory. Hypotheses, rumors, proposed actions and guesses must not become confirmed events; preserve their uncertainty if they are important to the current state.
