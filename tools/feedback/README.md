# Feedback forms

The app's feedback links open Google Forms, one per place that asks for feedback (TAS-458). This folder is an Apps
Script project that builds those forms and posts their answers to Slack:

- `Forms.gs` describes every form: its questions, sections, branching and prefill fields. It's the source of truth.
- `Sync.gs` makes the Google Forms match `Forms.gs`. It creates missing forms, updates the rest in place and gives
  each one a Slack trigger.
- `Slack.gs` posts each answer to a Slack channel.

The app reads each form's link from the `feedback_forms` Remote Config parameter (`config/FeedbackForms.kt`). A
place whose link is missing or empty shows no feedback link.

| Key | Form | Opened from (`{source}`) |
|---|---|---|
| `general` | Everlog feedback | Settings › Send feedback (`settings`) |
| `statistics` | Statistics feedback | Activity › Statistics tab (`activity_statistics`), exercise detail › Statistics tab (`exercise_statistics`) |
| `setTypes` | Set types feedback | Set type picker (`set_types`) |
| `setSuggestions` | Set suggestions feedback | Set suggestions screen (`set_suggestions`), old muscle goal screen (`muscle_goal`) |
| `workout` | Workout screen feedback | Workout screen ⋮ menu (`workout_menu`) |
| `exercisePicker` | Missing exercises | Exercise picker ⋮ menu (`exercise_picker_menu`), no results (`exercise_picker_no_results`) |

## How the forms work

- **Ids are what keeps a question.** `Sync.gs` remembers which Google Forms question belongs to each id in
  `Forms.gs` (in Script Properties: `form.<key>`, `item.<key>.<id>`, `link.<key>`). Changing a title or the
  choices updates the question in place, and its answers stay. Changing an id, or a question's type, deletes the
  question with its answers and adds a new one. A question that's no longer in `Forms.gs` is deleted with its
  answers, and so is anything added to a form by hand.
- **Branching goes between sections.** A section starts a new page. A choice can jump to a section by its id, or to
  `SUBMIT`; a choice without a jump goes on to the next section. Google Forms lets only one question per section
  jump, so keep it to one. The first section's title is the form's title.
- **Contact.** Every form ends with "Can we contact you about this?": Yes goes to the "Contact details" section
  (a required email address), No submits. It's required, otherwise skipping it would carry on to the email.
- **Prefill.** Google Forms has no hidden fields, so the app's details are ordinary short-answer questions people can
  see, optional and in the first section, which every path goes through. "App version" gets `{appVersion}`, and
  "Place" gets `{source}` on forms with more than one way in. The link in Remote Config is a Google Forms pre-filled
  link with those placeholders in it, braces as written (Google encodes them, `Sync.gs` puts them back), and the
  app fills them in when it opens the form. A `forms.gle` short link can't carry them.
- **The rest** is set on every form: the outro (`CONFIRMATION_MESSAGE`), a "Submit another response" link so
  people can send more than one, no limit on responses per person, published and accepting responses. New forms
  don't ask people to sign in.
- **Theme colour is set by hand.** No Google API can set a form's theme, so when a form is created, open it and set
  its colour under Customize theme (the palette icon). Syncing never changes it.

Copy follows the tone of voice guide in the "Tone of voice" section of the Product market fit page in Notion.

## Setting it up

Once, signed in with the Google account that owns the forms:

1. **Slack webhook.** At [api.slack.com/apps](https://api.slack.com/apps), in the "Everlog feedback" app, Incoming
   Webhooks › copy the channel's webhook URL (or Add New Webhook to Workspace › pick the channel). Treat it as a secret: anyone with it
   can post to the channel. Keep the channel private, as people's emails go into it.
2. **Project.** At [script.google.com](https://script.google.com), New project, "Everlog feedback forms". Add a
   script file for each `.gs` file here, with the same name, and paste it in. Delete the starter `Code.gs`.
3. **Script Properties** (Project Settings): `SLACK_WEBHOOK_URL`, the URL from step 1. Optionally
   `FORMS_FOLDER_ID`, the id of a Drive folder for new forms (the end of the folder's URL); without it they go in
   My Drive.
4. **First sync.** Follow "Changing a form" below. Google asks for access to forms, Drive, external requests and
   triggers, and warns that the app isn't verified: Advanced › Go to Everlog feedback forms.

## Changing a form

1. Change `Forms.gs` in a PR.
2. Paste the changed files into the Apps Script project.
3. Run `previewSync` and read the log. It lists every change without making it. Look out for deletions: they lose
   answers.
4. Run `syncForms`. The log ends with each form's edit link and the `feedback_forms` value, and says which links
   are new (a new form or a new prefill field).
5. Put new links in `feedback_forms` in Remote Config, dev and prod. Keep the braces as they are.
6. For a new form, set its theme colour.
7. Open each changed form from each of its places in the app, check App version and Place are filled in, submit it
   and check Slack. Debug builds fetch Remote Config every launch; release builds within an hour.

Failures show under Executions in the Apps Script project, and Google emails the owner when a trigger fails. The
triggers run as the account that ran `syncForms`, so that account needs to keep access to every form.
