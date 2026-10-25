// Posts each answer to Slack. Sync.gs gives every form a trigger that calls postToSlack when someone submits it.

function postToSlack(e) {
  const form = e.source;
  const answers = e.response.getItemResponses()
    .map((itemResponse) => ({
      question: itemResponse.getItem().getTitle(),
      answer: formatAnswer(itemResponse.getResponse()),
    }))
    .filter(({ answer }) => answer !== '')
    .map(({ question, answer }) => `*${escapeForSlack(question)}*\n${escapeForSlack(answer)}`);
  const text = [
    `New answer to *${escapeForSlack(form.getTitle())}*`,
    ...answers,
    `<${form.getEditUrl()}#responses|All responses>`,
  ].join('\n\n');

  UrlFetchApp.fetch(PropertiesService.getScriptProperties().getProperty('SLACK_WEBHOOK_URL'), {
    method: 'post',
    contentType: 'application/json',
    payload: JSON.stringify({ text }),
  });
}

// Checkboxes and grids answer with arrays, the rest with a string.
function formatAnswer(answer) {
  return Array.isArray(answer)
    ? answer.flat().filter(Boolean).join(', ')
    : String(answer ?? '').trim();
}

function escapeForSlack(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
