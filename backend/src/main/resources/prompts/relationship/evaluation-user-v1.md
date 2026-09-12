Current relationship:
stage={{stage}}
summary={{summary}}
interactionStyle={{interactionStyle}}
recentChange={{recentChange}}

Recent conversation:
USER: {{userMessage}}
ASSISTANT: {{assistantResponse}}

Return {"changed":false} when there is no meaningful relationship change. Otherwise return {"changed":true,"stage":"NEW|FAMILIAR|TRUSTED|CLOSE","summary":"...","interactionStyle":"...","recentChange":"..."}.

Stage may increase or decrease, but must be supported by this conversation. Do not invent shared events.
