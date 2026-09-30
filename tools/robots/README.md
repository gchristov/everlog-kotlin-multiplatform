# Pre-launch report Robo script

`everlog_robo_script.json` is the Robo script uploaded to the Play Console pre-launch report (Test and release → Testing → Pre-launch report → Settings → "Control how pre-launch report explores your app"). Play keeps only one script, and it can't be downloaded again once saved, so this is the copy to edit and re-upload.

The robot runs these steps first, then explores the app on its own:

1. Continue as guest. Test Lab runs are redirected to the Test Lab account (`AuthManager`).
2. Create a routine with three exercises, 12 reps each.
3. Create a plan with one week: the routine on Day 1, rest on the other days.
4. Start a workout from the routine, complete all its sets and finish.
5. Start an empty workout, add an exercise, complete its set and finish.

## What it relies on

- **Test Lab behaviour in the app:**
  - Routines, plans and workouts are kept in memory during Test Lab runs, so what the robot creates shows up in the app without being written to Firebase.
  - Onboarding tips are hidden, because the robot can't see or dismiss them.
  - Analytics is off.
- **Element IDs and texts:** steps find elements by resource ID and text, e.g. `completeBtn`, "Pick exercises as you go". Renaming one breaks the matching step.
- **Repeated elements:** where several elements match (the Reps and "Complete Set 1" buttons), the script taps the first one. Filling in or completing a set replaces that button, so the next tap reaches the next set.
- **Optional steps:** permission prompts and the "save as template" prompt are only tapped if they appear.

If a step fails, the robot goes back to exploring, and the pre-launch report's screenshots show where it stopped.
