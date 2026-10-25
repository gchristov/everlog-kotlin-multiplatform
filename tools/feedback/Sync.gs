// Makes the Google Forms match Forms.gs and gives each one a Slack trigger. previewSync logs what would change,
// syncForms changes it. See README.md.

const ITEM_TYPES = {
  paragraph: 'PARAGRAPH_TEXT',
  short: 'TEXT',
  prefill: 'TEXT',
  email: 'TEXT',
  choice: 'MULTIPLE_CHOICE',
  scale: 'SCALE',
  section: 'PAGE_BREAK',
};

function previewSync() {
  sync(false);
}

function syncForms() {
  sync(true);
}

function sync(apply) {
  checkForms();
  const store = PropertiesService.getScriptProperties();
  const formIds = FORMS.map((spec) => syncForm(spec, store, apply)).filter(Boolean);
  syncTriggers(formIds, apply);
  if (apply) {
    logLinks(store);
  }
}

function checkForms() {
  const keys = FORMS.map((spec) => spec.key);
  throwIfDuplicate(keys, 'form key');
  FORMS.forEach((spec) => {
    throwIfDuplicate(spec.items.map((item) => item.id), `${spec.key}: item id`);
    const sections = spec.items.filter((item) => item.type === 'section').map((item) => item.id);
    spec.items.forEach((item) => {
      if (!ITEM_TYPES[item.type]) {
        throw new Error(`${spec.key}: "${item.id}" has an unknown type, ${item.type}`);
      }
      (item.choices || []).map(choiceOf).forEach(({ goTo }) => {
        if (goTo && goTo !== SUBMIT && !sections.includes(goTo)) {
          throw new Error(`${spec.key}: "${item.id}" goes to "${goTo}", which isn't a section`);
        }
      });
    });
  });
}

function throwIfDuplicate(values, name) {
  const duplicate = values.find((value, index) => values.indexOf(value) !== index);
  if (duplicate) {
    throw new Error(`The ${name} "${duplicate}" is used more than once`);
  }
}

function syncForm(spec, store, apply) {
  const change = (message, action) => {
    console.log(`${apply ? '' : '[preview] '}${spec.key}: ${message}`);
    if (apply) {
      action();
    }
  };

  let form = openForm(store.getProperty(formKey(spec)));
  if (!form) {
    change(`create "${spec.title}" with a Slack trigger. Set its theme colour by hand afterwards.`, () => {
      form = createForm(spec, store);
    });
    if (!form) {
      return null;
    }
  }

  syncSettings(form, spec, change);
  syncItems(form, spec, store, apply, change);
  if (!form.isPublished()) {
    change('publish it', () => form.setPublished(true));
  }
  // An unpublished form can't say whether it accepts responses.
  if (form.isPublished() && !form.isAcceptingResponses()) {
    change('accept responses', () => form.setAcceptingResponses(true));
  }
  return form.getId();
}

function openForm(id) {
  if (!id) {
    return null;
  }
  try {
    return DriveApp.getFileById(id).isTrashed() ? null : FormApp.openById(id);
  } catch (error) {
    return null;
  }
}

function createForm(spec, store) {
  const form = FormApp.create(spec.title);
  store.setProperty(formKey(spec), form.getId());
  const folderId = store.getProperty('FORMS_FOLDER_ID');
  if (folderId) {
    DriveApp.getFileById(form.getId()).moveTo(DriveApp.getFolderById(folderId));
  }
  return form;
}

function syncSettings(form, spec, change) {
  const file = DriveApp.getFileById(form.getId());
  const settings = [
    ['file name', file.getName(), spec.fileName, () => file.setName(spec.fileName)],
    ['title', form.getTitle(), spec.title, () => form.setTitle(spec.title)],
    ['description', form.getDescription(), '', () => form.setDescription('')],
    ['outro', form.getConfirmationMessage(), CONFIRMATION_MESSAGE, () => form.setConfirmationMessage(CONFIRMATION_MESSAGE)],
    ['"Submit another response" link', form.hasRespondAgainLink(), true, () => form.setShowLinkToRespondAgain(true)],
    ['one response per person', form.hasLimitOneResponsePerUser(), false, () => form.setLimitOneResponsePerUser(false)],
  ];
  settings
    .filter(([, current, wanted]) => current !== wanted)
    .forEach(([name, current, wanted, set]) => change(`${name}: ${describe(current)} → ${describe(wanted)}`, set));
}

function syncItems(form, spec, store, apply, change) {
  // The form's items by their id in Forms.gs. In a preview, items that don't exist yet are missing.
  const items = {};
  const replaced = new Set();
  spec.items.forEach((item) => {
    let current = findItem(form, store, spec, item);
    if (current && String(current.getType()) !== ITEM_TYPES[item.type]) {
      replaced.add(current.getId());
      const old = current;
      change(`"${old.getTitle()}" is now a ${item.type}: delete it, with its answers, and add it again`, () => form.deleteItem(old));
      current = null;
    }
    if (current) {
      syncFields(typed(current), item, change, apply);
    } else {
      change(`add the ${item.type} "${item.title}"`, () => {
        current = addItem(form, item);
        store.setProperty(itemKey(spec, item), String(current.getId()));
        syncFields(typed(current), item, (message, action) => action(), apply);
      });
    }
    if (current) {
      items[item.id] = typed(current);
    }
  });

  spec.items
    .filter((item) => item.type === 'choice' && items[item.id])
    .forEach((item) => syncChoices(items[item.id], item, items, change));

  const kept = new Set(Object.values(items).map((item) => item.getId()));
  form.getItems()
    .filter((item) => !kept.has(item.getId()) && !replaced.has(item.getId()))
    .forEach((item) => change(`delete "${item.getTitle()}", with its answers`, () => form.deleteItem(item)));
  if (apply) {
    const ids = spec.items.map((item) => itemKey(spec, item));
    store.getKeys()
      .filter((key) => key.startsWith(`item.${spec.key}.`) && !ids.includes(key))
      .forEach((key) => store.deleteProperty(key));
  }

  if (Object.keys(items).length === spec.items.length) {
    const wanted = spec.items.map((item) => items[item.id].getId());
    const current = form.getItems().map((item) => item.getId()).filter((id) => wanted.includes(id));
    if (current.join() !== wanted.join()) {
      change('put the questions in order', () => wanted.forEach((id, index) => form.moveItem(form.getItemById(id), index)));
    }
  }
}

function findItem(form, store, spec, item) {
  const id = store.getProperty(itemKey(spec, item));
  return id ? form.getItemById(Number(id)) : null;
}

function addItem(form, item) {
  switch (item.type) {
    case 'paragraph':
      return form.addParagraphTextItem();
    case 'short':
    case 'prefill':
    case 'email':
      return form.addTextItem();
    case 'choice':
      return form.addMultipleChoiceItem();
    case 'scale':
      return form.addScaleItem();
    case 'section':
      return form.addPageBreakItem();
  }
}

function typed(item) {
  switch (String(item.getType())) {
    case 'PARAGRAPH_TEXT':
      return item.asParagraphTextItem();
    case 'TEXT':
      return item.asTextItem();
    case 'MULTIPLE_CHOICE':
      return item.asMultipleChoiceItem();
    case 'SCALE':
      return item.asScaleItem();
    case 'PAGE_BREAK':
      return item.asPageBreakItem();
    default:
      return item;
  }
}

function syncFields(current, item, change, apply) {
  const field = (name, now, wanted, set) => {
    if (now !== wanted) {
      change(`"${item.title}": ${name} ${describe(now)} → ${describe(wanted)}`, set);
    }
  };
  const helpText = item.type === 'prefill' ? PREFILL_HELP : (item.help || '');
  field('title', current.getTitle(), item.title, () => current.setTitle(item.title));
  field('description', current.getHelpText(), helpText, () => current.setHelpText(helpText));
  if (item.type !== 'section') {
    const required = item.type === 'email' || (item.type !== 'prefill' && !item.optional);
    field('required', current.isRequired(), required, () => current.setRequired(required));
  }
  if (item.type === 'scale') {
    const now = `${current.getLowerBound()}–${current.getUpperBound()}, ${current.getLeftLabel()}–${current.getRightLabel()}`;
    field('scale', now, `1–5, ${item.low}–${item.high}`, () => current.setBounds(1, 5).setLabels(item.low, item.high));
  }
  // Forms can't read a question's validation back, so it's set on every run without being logged.
  if (item.type === 'email' && apply) {
    current.setValidation(FormApp.createTextValidation().requireTextIsEmail().build());
  }
}

function syncChoices(current, item, items, change) {
  const choices = item.choices.map(choiceOf);
  const sectionIds = {};
  Object.entries(items).forEach(([id, formItem]) => {
    sectionIds[formItem.getId()] = id;
  });
  const now = current.getChoices().map((choice) => `${choice.getValue()} (${destinationOf(choice, sectionIds)})`)
    .concat(current.hasOtherOption() ? ['Other'] : []).join(', ');
  const wanted = choices.map(({ value, goTo }) => `${value} (${goTo || 'next'})`)
    .concat(item.other ? ['Other'] : []).join(', ');
  if (now !== wanted) {
    change(`"${item.title}": choices ${now || 'none'} → ${wanted}`, () => current
      .setChoices(choices.map(({ value, goTo }) => newChoice(current, value, goTo, items)))
      .showOtherOption(Boolean(item.other)));
  }
}

function choiceOf(choice) {
  return typeof choice === 'string' ? { value: choice } : choice;
}

function destinationOf(choice, sectionIds) {
  const section = choice.getGotoPage();
  if (section) {
    return sectionIds[section.getId()] || `item ${section.getId()}`;
  }
  return String(choice.getPageNavigationType()) === 'SUBMIT' ? SUBMIT : 'next';
}

function newChoice(item, value, goTo, items) {
  if (!goTo) {
    return item.createChoice(value);
  }
  return item.createChoice(value, goTo === SUBMIT ? FormApp.PageNavigationType.SUBMIT : items[goTo]);
}

// One Slack trigger per form in Forms.gs, and none for anything else.
function syncTriggers(formIds, apply) {
  const change = (message, action) => {
    console.log(`${apply ? '' : '[preview] '}${message}`);
    if (apply) {
      action();
    }
  };
  const watched = new Set();
  ScriptApp.getProjectTriggers()
    .filter((trigger) => trigger.getHandlerFunction() === 'postToSlack')
    .forEach((trigger) => {
      const formId = trigger.getTriggerSourceId();
      if (!formIds.includes(formId) || watched.has(formId)) {
        change(`remove the Slack trigger on form ${formId}`, () => ScriptApp.deleteTrigger(trigger));
      } else {
        watched.add(formId);
      }
    });
  formIds
    .filter((formId) => !watched.has(formId))
    .forEach((formId) => change(`add a Slack trigger to form ${formId}`, () => ScriptApp.newTrigger('postToSlack')
      .forForm(formId).onFormSubmit().create()));
}

// Logs each form's edit link and the feedback_forms value with the prefill links, and says which links changed.
function logLinks(store) {
  const links = {};
  FORMS.forEach((spec) => {
    const form = FormApp.openById(store.getProperty(formKey(spec)));
    const response = form.createResponse();
    spec.items.filter((item) => item.type === 'prefill').forEach((item) => {
      const field = form.getItemById(Number(store.getProperty(itemKey(spec, item)))).asTextItem();
      response.withItemResponse(field.createResponse(item.value));
    });
    // The app fills the placeholders in as written, so the braces Google encodes are put back.
    links[spec.key] = response.toPrefilledUrl().replace(/%7B(appVersion|source)%7D/gi, '{$1}');
    console.log(`${spec.key}: ${form.getEditUrl()}`);
    if (store.getProperty(linkKey(spec)) !== links[spec.key]) {
      console.log(`${spec.key}: the link is new, so feedback_forms in Remote Config needs it`);
      store.setProperty(linkKey(spec), links[spec.key]);
    }
  });
  console.log(`feedback_forms:\n${JSON.stringify(links, null, 2)}`);
}

function formKey(spec) {
  return `form.${spec.key}`;
}

function itemKey(spec, item) {
  return `item.${spec.key}.${item.id}`;
}

function linkKey(spec) {
  return `link.${spec.key}`;
}

function describe(value) {
  return value === '' ? 'empty' : JSON.stringify(value);
}
