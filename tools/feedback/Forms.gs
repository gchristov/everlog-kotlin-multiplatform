// The feedback forms. Sync.gs makes the Google Forms match this file: change a form here, then follow "Changing
// a form" in README.md.
//
// key is the form's key in the app's feedback_forms Remote Config parameter. Each item's id is how Sync.gs finds
// the question again on the next run, so changing a title keeps the question and its answers, while changing an id
// replaces it with a new, empty one. A question is required unless it's optional. A section starts a new page, and
// a choice can jump to a section by its id, or to SUBMIT.

const SUBMIT = 'submit';

const CONFIRMATION_MESSAGE = 'Thanks, that’s sent. We read every answer, and if you left your email we might get in '
  + 'touch about it. You can close this page and go back to Everlog.';

const PREFILL_HELP = 'Filled in by the app. Leave it as it is.';

// The app fills {appVersion} and {source} into the form's link when it opens it (config/FeedbackForms.kt).
const APP_VERSION = { id: 'appVersion', type: 'prefill', title: 'App version', value: '{appVersion}' };
const PLACE = { id: 'place', type: 'prefill', title: 'Place', value: '{source}' };

const CONTACT = {
  id: 'contact',
  type: 'choice',
  title: 'Can we contact you about this?',
  choices: [{ value: 'Yes', goTo: 'contactDetails' }, { value: 'No', goTo: SUBMIT }],
};
const CONTACT_DETAILS = [
  { id: 'contactDetails', type: 'section', title: 'Contact details' },
  { id: 'email', type: 'email', title: 'Your email' },
];

const FORMS = [
  {
    key: 'general',
    title: 'Everlog feedback',
    fileName: 'Everlog: App general feedback',
    items: [
      {
        id: 'topic',
        type: 'choice',
        title: 'What\'s it about?',
        choices: [
          { value: 'An idea or request', goTo: 'contactSection' },
          { value: 'Something isn\'t working', goTo: 'problem' },
          { value: 'Something else', goTo: 'contactSection' },
        ],
      },
      { id: 'details', type: 'paragraph', title: 'Tell us more' },
      APP_VERSION,
      { id: 'problem', type: 'section', title: 'About the problem' },
      { id: 'whatHappened', type: 'paragraph', title: 'What were you doing when it happened?', optional: true },
      { id: 'frequency', type: 'choice', title: 'Does it happen every time?', choices: ['Every time', 'Sometimes', 'Once'] },
      // Both of the first question's branches end up here, so the contact question gets a section of its own.
      { id: 'contactSection', type: 'section', title: 'Contact' },
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
  {
    key: 'statistics',
    title: 'Statistics feedback',
    fileName: 'Everlog: App statistics feedback',
    items: [
      { id: 'wanted', type: 'paragraph', title: 'Which stats would you like to see?' },
      {
        id: 'use',
        type: 'choice',
        title: 'What do you use the stats for today?',
        choices: ['Tracking progress on lifts', 'Weekly volume', 'Checking how often I train', 'I rarely look at them'],
        other: true,
      },
      { id: 'problems', type: 'paragraph', title: 'Is anything wrong or hard to read?', optional: true },
      APP_VERSION,
      PLACE,
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
  {
    key: 'setTypes',
    title: 'Set types feedback',
    fileName: 'Everlog: App set types feedback',
    items: [
      { id: 'wanted', type: 'short', title: 'Which set type would you like to log?' },
      { id: 'today', type: 'paragraph', title: 'How do you log it at the moment?', optional: true },
      APP_VERSION,
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
  {
    key: 'setSuggestions',
    title: 'Set suggestions feedback',
    fileName: 'Everlog: App set suggestions feedback',
    items: [
      { id: 'mode', type: 'choice', title: 'Which do you use?', choices: ['Off', 'From last time', 'Train for a goal'] },
      {
        id: 'weights',
        type: 'choice',
        title: 'Were the suggested weights right for you?',
        choices: ['Too light', 'About right', 'Too heavy', 'It varies'],
      },
      { id: 'goals', type: 'paragraph', title: 'Which goals would you like to train for that aren\'t there?', optional: true },
      { id: 'other', type: 'paragraph', title: 'Anything else about set suggestions?', optional: true },
      APP_VERSION,
      PLACE,
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
  {
    key: 'workout',
    title: 'Workout screen feedback',
    fileName: 'Everlog: App workout screen feedback',
    items: [
      { id: 'ease', type: 'scale', title: 'How do you find logging a workout on this screen?', low: 'Hard', high: 'Easy' },
      { id: 'good', type: 'paragraph', title: 'What works well?', optional: true },
      { id: 'missing', type: 'paragraph', title: 'What\'s missing or getting in the way?' },
      { id: 'slower', type: 'paragraph', title: 'Is anything slower than it used to be?', optional: true },
      APP_VERSION,
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
  {
    key: 'exercisePicker',
    title: 'Missing exercises',
    fileName: 'Everlog: App exercise picker feedback',
    items: [
      { id: 'exercise', type: 'short', title: 'Which exercise couldn\'t you find?' },
      { id: 'equipment', type: 'short', title: 'What equipment does it use?', optional: true },
      { id: 'other', type: 'paragraph', title: 'Anything else about finding exercises?', optional: true },
      APP_VERSION,
      PLACE,
      CONTACT,
      ...CONTACT_DETAILS,
    ],
  },
];
