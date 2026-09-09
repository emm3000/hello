# Delete your Hello account and data

**App:** Hello (`com.emm.hello`)
**Developer:** Edgardo Muñoz
**Last updated: 2026-09-08**

Hello stores your decks, flashcards and study history on your device. An anonymous backend account is created only the first time you use an AI feature, and you may optionally link a Google account to it from Settings.

## How to request deletion

1. Send an email to **edgardo.emm20@gmail.com** with the subject `Delete my Hello account`.
2. In the message, tell us how to identify your account:
   - If you linked a Google account, send the email from that same Google address.
   - If you did not link an account, describe your installation and the approximate dates on which you used the AI features.
3. We confirm the request and delete the data within 30 days.

You do not need the app installed to make this request.

## Delete your on-device data yourself

Everything Hello keeps locally is removed when you uninstall the app. You can also delete individual decks and flashcards from inside the app at any time.

## What is deleted

- Your anonymous backend account and any linked Google identity.
- Your generation event rows, which record the date, the operation, the provider and the outcome of each AI request. They never contain the text you submitted.

## What is kept, and for how long

- **Generated note cache.** Cached results are stored under a hash of the request text and the generation parameters, with no user id attached. They cannot be linked back to you and are retained for service operation. Cached refusals expire 24 hours after they are written.
- **Backend logs.** Operational log lines record the operation, the outcome and the elapsed time. They contain no submitted text and no account id, and are retained under the hosting platform's default retention for function logs.
- **Crash and analytics data.** Crash reports collected by Firebase Crashlytics are retained for 90 days. Analytics data is retained under Firebase default settings. To have this data deleted as well, say so in the same email and we will submit the deletion request through the Firebase console.

## Related

See the [Privacy Policy](privacy-policy.html) for the full description of what Hello collects and why.
