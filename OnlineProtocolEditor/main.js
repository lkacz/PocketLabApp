const recognizedCommands = new Set([
  "BODY_ALIGNMENT",
  "BODY_COLOR",
  "BODY_SIZE",
  "CONTINUE_TEXT_COLOR",
  "CONTINUE_ALIGNMENT",
  "CONTINUE_BACKGROUND_COLOR",
  "CONTINUE_SIZE",
  "HTML",
  "END",
  "GOTO",
  "HEADER_ALIGNMENT",
  "HEADER_COLOR",
  "HEADER_SIZE",
  "INPUTFIELD",
  "INPUTFIELD[RANDOMIZED]",
  "INSTRUCTION",
  "ITEM_SIZE",
  "LABEL",
  "LOG",
  "RANDOMIZE_OFF",
  "RANDOMIZE_ON",
  "RESPONSE_BACKGROUND_COLOR",
  "RESPONSE_SIZE",
  "RESPONSE_TEXT_COLOR",
  "SCALE",
  "SCALE[RANDOMIZED]",
  "SCREEN_BACKGROUND_COLOR",
  "STUDY_ID",
  "TIMER",
  "TIMER_SOUND",
  "TIMER_SIZE",
  "TIMER_COLOR",
  "TIMER_ALIGNMENT",
  "TRANSITIONS",
]);

const alignmentCommands = new Set([
  "BODY_ALIGNMENT",
  "CONTINUE_ALIGNMENT",
  "HEADER_ALIGNMENT",
  "TIMER_ALIGNMENT",
]);

const sizeCommands = new Set([
  "BODY_SIZE",
  "CONTINUE_SIZE",
  "HEADER_SIZE",
  "RESPONSE_SIZE",
  "ITEM_SIZE",
  "TIMER_SIZE",
]);

const colorCommands = new Set([
  "BODY_COLOR",
  "CONTINUE_TEXT_COLOR",
  "CONTINUE_BACKGROUND_COLOR",
  "HEADER_COLOR",
  "RESPONSE_TEXT_COLOR",
  "RESPONSE_BACKGROUND_COLOR",
  "SCREEN_BACKGROUND_COLOR",
  "TIMER_COLOR",
]);

const commandsWithMedia = new Set([
  "INSTRUCTION",
  "TIMER",
  "SCALE",
  "SCALE[RANDOMIZED]",
  "INPUTFIELD",
  "INPUTFIELD[RANDOMIZED]",
]);

const holdSupportedCommands = new Set([
  "INSTRUCTION",
  "TIMER",
  "INPUTFIELD",
  "INPUTFIELD[RANDOMIZED]",
  "HTML",
]);

const randomizablePairs = {
  SCALE: "SCALE[RANDOMIZED]",
  "SCALE[RANDOMIZED]": "SCALE",
  INPUTFIELD: "INPUTFIELD[RANDOMIZED]",
  "INPUTFIELD[RANDOMIZED]": "INPUTFIELD",
};

const COMMAND_CATEGORIES = ["All", "Content", "Randomization", "Meta", "Style"];

function friendlyCommandName(command) {
  return command
    .replace(/\[RANDOMIZED]$/i, " (randomized)")
    .replace(/_/g, " ")
    .replace(/\[/g, " (")
    .replace(/]/g, ")")
    .toLowerCase()
    .replace(/\b\w/g, (ch) => ch.toUpperCase());
}

const commandDefinitions = {
  INSTRUCTION: {
    category: "Content",
    description: "Display instructional header and body text with an optional continue action and media attachments.",
    builder: buildInstructionHandler,
  },
  TIMER: {
    category: "Content",
    description: "Configure a countdown timer with header/body copy, duration, optional media, and continue behavior.",
    builder: buildTimerHandler,
  },
  SCALE: {
    category: "Content",
    description: "Present a scale with item prompts and response options. Toggle randomization for responses if needed.",
    builder: (ctx) => buildScaleHandler({ ...ctx, initialRandomized: false }),
  },
  "SCALE[RANDOMIZED]": {
    category: "Content",
    description: "Present a scale with randomized item order. Same options as SCALE with randomization enabled by default.",
    builder: (ctx) => buildScaleHandler({ ...ctx, initialRandomized: true }),
  },
  INPUTFIELD: {
    category: "Content",
    description: "Collect text input across one or more fields with optional media and continue button.",
    builder: (ctx) => buildInputFieldHandler({ ...ctx, initialRandomized: false }),
  },
  "INPUTFIELD[RANDOMIZED]": {
    category: "Content",
    description: "Collect randomized text input fields. Same options as INPUTFIELD with randomization on.",
    builder: (ctx) => buildInputFieldHandler({ ...ctx, initialRandomized: true }),
  },
  LABEL: {
    category: "Content",
    description: "Define a label that can be targeted by GOTO commands.",
    builder: buildLabelHandler,
  },
  GOTO: {
    category: "Content",
    description: "Jump to a previously defined label in the protocol.",
    builder: buildGotoHandler,
  },
  HTML: {
    category: "Content",
    description: "Embed an HTML asset and optionally require a continue confirmation.",
    builder: buildHtmlHandler,
  },
  TIMER_SOUND: {
    category: "Content",
    description: "Specify the audio file that plays when a timer completes.",
    builder: buildTimerSoundHandler,
  },
  LOG: {
    category: "Content",
    description: "Write an arbitrary message into the session log for debugging.",
    builder: buildLogHandler,
  },
  END: {
    category: "Content",
    description: "Mark the end of the protocol.",
    builder: buildEndHandler,
  },
  RANDOMIZE_ON: {
    category: "Randomization",
    description: "Begin a randomization block. Commands until RANDOMIZE_OFF will be shuffled.",
    builder: buildRandomizeOnHandler,
  },
  RANDOMIZE_OFF: {
    category: "Randomization",
    description: "Close the most recent RANDOMIZE_ON block.",
    builder: buildRandomizeOffHandler,
  },
  STUDY_ID: {
    category: "Meta",
    description: "Set the STUDY_ID used for log syncing.",
    builder: buildStudyIdHandler,
  },
  TRANSITIONS: {
    category: "Meta",
    description: "Choose the screen transition animation for subsequent slides.",
    builder: buildTransitionsHandler,
  },
};

colorCommands.forEach((command) => {
  if (!commandDefinitions[command]) {
    commandDefinitions[command] = {
      category: "Style",
      description: `Set ${friendlyCommandName(command)} using a hex color such as #RRGGBB or #AARRGGBB.`,
      builder: (ctx) => buildColorHandler({ ...ctx, commandName: command }),
    };
  }
});

sizeCommands.forEach((command) => {
  if (!commandDefinitions[command]) {
    commandDefinitions[command] = {
      category: "Style",
      description: `Set ${friendlyCommandName(command)} with a positive number (points).`,
      builder: (ctx) => buildSizeHandler({ ...ctx, commandName: command }),
    };
  }
});

alignmentCommands.forEach((command) => {
  if (!commandDefinitions[command]) {
    commandDefinitions[command] = {
      category: "Style",
      description: `Set ${friendlyCommandName(command)} to LEFT, CENTER, or RIGHT.`,
      builder: (ctx) => buildAlignmentHandler({ ...ctx, commandName: command }),
    };
  }
});

const protocolInput = document.getElementById("protocolInput");
const lineCountLabel = document.getElementById("lineCount");
const statusText = document.getElementById("statusText");
const unsavedIndicator = document.getElementById("unsavedIndicator");
const autoValidateToggle = document.getElementById("autoValidate");
const validateButton = document.getElementById("validateButton");
const issueFilterInput = document.getElementById("issueFilter");
const validationTableBody = document.querySelector("#validationTable tbody");
const prevIssueButton = document.getElementById("prevIssue");
const nextIssueButton = document.getElementById("nextIssue");
const commandList = document.getElementById("commandList");
const toggleCommandsButton = document.getElementById("toggleCommands");
const commandsBody = document.getElementById("commandsBody");
const newButton = document.getElementById("newButton");
const loadButton = document.getElementById("loadButton");
const saveButton = document.getElementById("saveButton");
const downloadButton = document.getElementById("downloadButton");
const insertButton = document.getElementById("insertButton");
const fileInput = document.getElementById("fileInput");
const insertDialog = document.getElementById("insertDialog");
const insertForm = insertDialog ? insertDialog.querySelector(".insert-form") : null;
const insertDialogTitle = document.getElementById("insertDialogTitle");
const insertDialogSubtitle = document.getElementById("insertDialogSubtitle");
const insertCategorySelect = document.getElementById("insertCategory");
const insertSearchInput = document.getElementById("insertSearch");
const insertCommandList = document.getElementById("insertCommandList");
const selectedCommandNameEl = document.getElementById("selectedCommandName");
const clearCommandSelectionBtn = document.getElementById("clearCommandSelection");
const configPlaceholder = document.getElementById("configPlaceholder");
const configSections = document.getElementById("configSections");
const commandHintEl = document.getElementById("commandHint");
const parameterContainer = document.getElementById("parameterContainer");
const insertDialogMessage = document.getElementById("insertDialogMessage");
const insertDialogSubmit = document.getElementById("insertDialogSubmit");

const insertDialogElementsReady = Boolean(
  insertDialog &&
  insertForm &&
  insertCategorySelect &&
  insertSearchInput &&
  insertCommandList &&
  selectedCommandNameEl &&
  clearCommandSelectionBtn &&
  configPlaceholder &&
  configSections &&
  commandHintEl &&
  parameterContainer &&
  insertDialogMessage &&
  insertDialogSubmit,
);

let insertDialogElementsMissingLogged = false;

function ensureInsertDialogElements() {
  if (insertDialogElementsReady) return true;
  if (!insertDialogElementsMissingLogged) {
    console.warn("Insert command dialog elements are missing; disabling insert dialog features.");
    insertDialogElementsMissingLogged = true;
  }
  return false;
}

let currentFileHandle = null;
let currentFileName = "Untitled";
let isDirty = false;
let validationResults = [];
let filteredIndices = [];
let selectedIssueIndex = -1;
let debounceTimer = null;

function debounce(fn, delay = 250) {
  return (...args) => {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => fn(...args), delay);
  };
}

function appendMessage(current, add) {
  if (!current) return add;
  if (!add) return current;
  return `${current}; ${add}`;
}

function parseFieldTokens(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return [];
  if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length >= 2) {
    const inner = trimmed.slice(1, -1);
    return inner
      .split(/[;,]/)
      .map((token) => token.trim())
      .filter(Boolean);
  }
  return trimmed
    .split(/[;,]/)
    .map((token) => token.trim())
    .filter(Boolean);
}

function findLabelOccurrences(lines) {
  const map = new Map();
  lines.forEach((raw, index) => {
    const trimmed = raw.trim();
    if (trimmed.toUpperCase().startsWith("LABEL;")) {
      const parts = trimmed.split(";");
      const name = (parts[1] || "").trim();
      if (name) {
        if (!map.has(name)) map.set(name, []);
        map.get(name).push(index + 1);
      }
    }
  });
  return map;
}

function handleKnown(command, parts, lineNumber, line, labels) {
  let error = "";
  let warning = "";
  if (command === "LABEL") {
    const labelName = (parts[1] || "").trim();
    const occurrences = labels.get(labelName) || [];
    if (occurrences.length > 1 && occurrences.includes(lineNumber)) {
      const others = occurrences.filter((n) => n !== lineNumber);
      if (others.length) {
        error = appendMessage(error, `Label duplicated with line(s) ${others.join(", ")}`);
      }
    }
  } else if (command === "GOTO") {
    const target = (parts[1] || "").trim();
    if (!target) {
      error = appendMessage(error, "GOTO missing target label");
    } else if (!labels.has(target)) {
      error = appendMessage(error, `GOTO target label '${target}' not defined`);
    }
  } else if (command === "INSTRUCTION") {
    if ((line.match(/;/g) || []).length !== 3) {
      error = appendMessage(error, "INSTRUCTION must have exactly 3 semicolons (4 segments)");
    }
  } else if (command === "INPUTFIELD" || command === "INPUTFIELD[RANDOMIZED]") {
    if (parts.length < 4) {
      error = appendMessage(error, `${command} must have at least 4 segments`);
    }
    const tail = parts.slice(3);
    let candidateSegments;
    if (!tail.length) {
      candidateSegments = [];
    } else if (tail.length === 1) {
      candidateSegments = tail;
    } else {
      const last = tail[tail.length - 1].trim();
      const looksLikeContinue = /\b(continue|complete|next)\b/i.test(last) || /\[HOLD]/i.test(last);
      candidateSegments = looksLikeContinue ? tail.slice(0, -1) : tail;
      if (!candidateSegments.length) candidateSegments = tail;
    }
    const definedFields = candidateSegments.flatMap(parseFieldTokens);
    if (!definedFields.length) {
      error = appendMessage(error, `${command} needs at least one field definition in 4th segment`);
    }
    if (command === "INPUTFIELD[RANDOMIZED]" && definedFields.length < 2) {
      warning = appendMessage(warning, "Randomized input field has fewer than 2 fields");
    }
  } else if (command === "SCALE" || command === "SCALE[RANDOMIZED]") {
    if (parts.length < 2) {
      error = appendMessage(error, `${command} must have at least one parameter after the command`);
    }
  } else if (command === "TIMER") {
    if (parts.length !== 5) {
      error = appendMessage(error, "TIMER must have exactly 4 semicolons (5 segments)");
    }
    const timeVal = parseInt((parts[3] || "").trim(), 10);
    if (Number.isNaN(timeVal) || timeVal < 0) {
      error = appendMessage(error, "TIMER must have a non-negative integer in the 4th segment");
    } else if (timeVal > 3600) {
      warning = appendMessage(warning, `TIMER = ${timeVal} (over 3600s, is that intentional?)`);
    }
  }

  if (alignmentCommands.has(command)) {
    const valSegment = ((parts[1] || "").trim().toUpperCase());
    if (!valSegment) {
      error = appendMessage(error, `${command} requires a value`);
    } else if (!["LEFT", "CENTER", "RIGHT"].includes(valSegment)) {
      error = appendMessage(error, `${command} invalid alignment '${valSegment}'`);
    }
  }
  if (sizeCommands.has(command)) {
    const sizeVal = parseFloat((parts[1] || "").trim());
    if (!Number.isFinite(sizeVal) || sizeVal <= 0) {
      error = appendMessage(error, `${command} must be a positive number`);
    } else if (sizeVal > 200) {
      warning = appendMessage(warning, `${command}=${sizeVal} unusually large`);
    }
  }
  if (colorCommands.has(command)) {
    const colorVal = (parts[1] || "").trim();
    const colorOk = /^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(colorVal);
    if (!colorOk) {
      error = appendMessage(error, `${command} expects hex color like #RRGGBB or #AARRGGBB`);
    }
  }
  return { error, warning };
}

function validateProtocol(text) {
  const lines = text.split(/\r?\n/);
  const labelOccurrences = findLabelOccurrences(lines);
  let randomizationLevel = 0;
  let lastCommand = null;
  let studyIdSeen = false;
  const results = [];

  lines.forEach((raw, index) => {
    const trimmed = raw.trim();
    let error = "";
    let warning = "";
    let command = "";

    if (!trimmed || trimmed.startsWith("//")) {
      lastCommand = null;
      results.push({ lineNumber: index + 1, raw, command, error, warning });
      return;
    }

    if (trimmed.endsWith(";")) {
      error = appendMessage(error, "Line ends with stray semicolon");
    }

    const parts = trimmed.split(";");
    command = (parts[0] || "").toUpperCase();
    const recognized = recognizedCommands.has(command);

    switch (command) {
      case "RANDOMIZE_ON":
        if (lastCommand === "RANDOMIZE_ON") {
          error = appendMessage(error, "RANDOMIZE_ON cannot follow RANDOMIZE_ON");
        }
        randomizationLevel += 1;
        break;
      case "RANDOMIZE_OFF":
        if (randomizationLevel <= 0) {
          error = appendMessage(error, "RANDOMIZE_OFF without matching RANDOMIZE_ON");
        } else {
          randomizationLevel -= 1;
        }
        break;
      case "STUDY_ID":
        if (studyIdSeen) {
          error = appendMessage(error, "Duplicate STUDY_ID");
        } else {
          studyIdSeen = true;
          if (!((parts[1] || "").trim())) {
            error = appendMessage(error, "STUDY_ID missing required value");
          }
        }
        break;
      default:
        break;
    }

    if (!recognized) {
      error = appendMessage(error, "Unrecognized command");
    } else {
      const handled = handleKnown(command, parts, index + 1, trimmed, labelOccurrences);
      if (handled.error) error = appendMessage(error, handled.error);
      if (handled.warning) warning = appendMessage(warning, handled.warning);
    }

    lastCommand = command;
    results.push({ lineNumber: index + 1, raw, command, error, warning });
  });

  if (randomizationLevel > 0) {
    results.push({
      lineNumber: lines.length + 1,
      raw: "<EOF>",
      command: "",
      error: "RANDOMIZE_ON not closed by matching RANDOMIZE_OFF",
      warning: "",
    });
  }

  return results;
}

function updateLineCount() {
  const lines = protocolInput.value.split(/\r?\n/);
  lineCountLabel.textContent = `${lines.length} line${lines.length === 1 ? "" : "s"}`;
}

function markDirty() {
  if (!isDirty) {
    isDirty = true;
    unsavedIndicator.hidden = false;
  }
}

function markSaved(message = "All changes saved") {
  isDirty = false;
  unsavedIndicator.hidden = true;
  setStatus(message);
}

function setStatus(message) {
  statusText.textContent = message;
  if (message) {
    statusText.classList.add("status-active");
    setTimeout(() => statusText.classList.remove("status-active"), 2500);
  }
}

function renderValidationResults() {
  const filter = issueFilterInput.value.trim().toLowerCase();
  validationTableBody.innerHTML = "";
  filteredIndices = [];

  const isLineFilter = /^#?\d+$/.test(filter);
  const targetLine = isLineFilter ? parseInt(filter.replace("#", ""), 10) : null;

  validationResults.forEach((result, index) => {
    if (!filter) {
      filteredIndices.push(index);
    } else if (isLineFilter && targetLine !== null) {
      if (result.lineNumber === targetLine) filteredIndices.push(index);
      else return;
    } else {
      const haystack = `${result.lineNumber} ${result.command} ${result.error} ${result.warning} ${result.raw}`.toLowerCase();
      if (!haystack.includes(filter)) return;
      filteredIndices.push(index);
    }

    const tr = document.createElement("tr");
    tr.dataset.index = String(index);
    tr.addEventListener("click", () => focusLine(result.lineNumber));

    const lineCell = document.createElement("td");
    lineCell.textContent = result.lineNumber;
    tr.append(lineCell);

    const commandCell = document.createElement("td");
    commandCell.textContent = result.command;
    tr.append(commandCell);

    const errorCell = document.createElement("td");
    errorCell.textContent = result.error;
    if (result.error) errorCell.classList.add("validation-error");
    tr.append(errorCell);

    const warningCell = document.createElement("td");
    warningCell.textContent = result.warning;
    if (result.warning) warningCell.classList.add("validation-warning");
    tr.append(warningCell);

    validationTableBody.append(tr);
  });

  if (filteredIndices.length === 0) {
    const emptyRow = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 4;
    cell.textContent = validationResults.length ? "No results match this filter." : "Run validation to see results.";
    emptyRow.append(cell);
    validationTableBody.append(emptyRow);
  }

  selectedIssueIndex = filteredIndices.length ? 0 : -1;
  highlightSelectedIssue();
}

function highlightSelectedIssue(focusLineTarget = false) {
  const rows = validationTableBody.querySelectorAll("tr");
  rows.forEach((row) => row.classList.remove("selected"));
  if (selectedIssueIndex < 0 || selectedIssueIndex >= filteredIndices.length) return;
  const targetIndex = filteredIndices[selectedIssueIndex];
  const row = validationTableBody.querySelector(`tr[data-index="${targetIndex}"]`);
  if (row) {
    row.classList.add("selected");
    if (focusLineTarget) {
      const result = validationResults[targetIndex];
      if (result && result.lineNumber) focusLine(result.lineNumber);
    }
  }
}

function focusLine(lineNumber) {
  if (!lineNumber || lineNumber < 1) return;
  const lines = protocolInput.value.split(/\r?\n/);
  let offset = 0;
  for (let i = 0; i < lineNumber - 1 && i < lines.length; i += 1) {
    offset += lines[i].length + 1;
  }
  protocolInput.focus();
  const lineText = lines[lineNumber - 1] || "";
  protocolInput.setSelectionRange(offset, offset + lineText.length);
}

function goToRelativeIssue(delta) {
  if (!filteredIndices.length) return;
  selectedIssueIndex = (selectedIssueIndex + delta + filteredIndices.length) % filteredIndices.length;
  highlightSelectedIssue(true);
}

const debouncedValidate = debounce(() => {
  validationResults = validateProtocol(protocolInput.value);
  renderValidationResults();
  const issues = validationResults.filter((r) => r.error);
  if (issues.length) {
    setStatus(`${issues.length} error${issues.length === 1 ? "" : "s"} found`);
  } else {
    const warnings = validationResults.filter((r) => r.warning);
    if (warnings.length) {
      setStatus(`${warnings.length} warning${warnings.length === 1 ? "" : "s"} detected`);
    } else {
      setStatus("Protocol looks good");
    }
  }
}, 300);

function triggerValidation() {
  validationResults = validateProtocol(protocolInput.value);
  renderValidationResults();
  const issueCount = validationResults.filter((r) => r.error).length;
  const warningCount = validationResults.filter((r) => r.warning).length;
  if (issueCount) {
    setStatus(`${issueCount} error${issueCount === 1 ? "" : "s"} found`);
  } else if (warningCount) {
    setStatus(`${warningCount} warning${warningCount === 1 ? "" : "s"} detected`);
  } else {
    setStatus("Protocol looks good");
  }
}

function insertAtCursor(value) {
  const start = protocolInput.selectionStart ?? protocolInput.value.length;
  const end = protocolInput.selectionEnd ?? protocolInput.value.length;
  const before = protocolInput.value.slice(0, start);
  const after = protocolInput.value.slice(end);
  const needsNewlineBefore = before && !before.endsWith("\n");
  const prefix = needsNewlineBefore ? "\n" : "";
  const newValue = `${before}${prefix}${value}\n${after}`;
  protocolInput.value = newValue;
  const cursorPos = before.length + prefix.length + value.length + 1;
  protocolInput.setSelectionRange(cursorPos, cursorPos);
  protocolInput.focus();
  updateLineCount();
  markDirty();
  if (autoValidateToggle.checked) debouncedValidate();
}

function populateCommandsList() {
  const sortedCommands = Object.keys(commandDefinitions).sort((a, b) => a.localeCompare(b));
  commandList.innerHTML = "";
  sortedCommands.forEach((command) => {
    const li = document.createElement("li");
    li.tabIndex = 0;
    li.textContent = command;
    li.addEventListener("click", () => openInsertDialog(command));
    li.addEventListener("keypress", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        openInsertDialog(command);
      }
    });
    commandList.append(li);
  });
}

let insertDialogInitialized = false;
let dialogCommandEntries = [];
let currentInsertCommand = null;
let currentInsertHandler = null;

function initializeInsertDialog() {
  if (!ensureInsertDialogElements()) return;
  if (insertDialogInitialized) return;
  insertDialogInitialized = true;

  insertCategorySelect.innerHTML = "";
  COMMAND_CATEGORIES.forEach((category) => {
    const option = document.createElement("option");
    option.value = category;
    option.textContent = category;
    insertCategorySelect.append(option);
  });

  const categoryIndex = (category) => {
    const idx = COMMAND_CATEGORIES.indexOf(category);
    return idx === -1 ? COMMAND_CATEGORIES.length : idx;
  };

  dialogCommandEntries = Object.keys(commandDefinitions)
    .map((name) => ({
      name,
      category: commandDefinitions[name]?.category || "Content",
    }))
    .sort((a, b) => {
      const catDiff = categoryIndex(a.category) - categoryIndex(b.category);
      if (catDiff !== 0) return catDiff;
      return a.name.localeCompare(b.name);
    })
    .map((entry) => {
      const listItem = document.createElement("li");
      const button = document.createElement("button");
      button.type = "button";
      button.className = "command-item";
      button.textContent = entry.name;
      button.addEventListener("click", () => selectCommand(entry.name));
      listItem.append(button);
      insertCommandList.append(listItem);
      return { ...entry, button, listItem };
    });

  insertCategorySelect.addEventListener("change", applyInsertFilters);
  insertSearchInput.addEventListener("input", applyInsertFilters);
  clearCommandSelectionBtn.addEventListener("click", () => {
    clearSelectedCommand();
    insertSearchInput.focus();
  });
}

function applyInsertFilters() {
  if (!ensureInsertDialogElements()) return;
  const category = insertCategorySelect.value;
  const query = insertSearchInput.value.trim().toLowerCase();
  dialogCommandEntries.forEach((entry) => {
    const matchesCategory = category === "All" || entry.category === category;
    const matchesQuery = !query || entry.name.toLowerCase().includes(query);
    entry.listItem.hidden = !(matchesCategory && matchesQuery);
  });
}

function setActiveCommandButton(commandName) {
  if (!ensureInsertDialogElements()) return;
  dialogCommandEntries.forEach((entry) => {
    const isActive = Boolean(commandName && entry.name === commandName);
    if (isActive) {
      entry.button.classList.add("active");
      entry.button.setAttribute("aria-selected", "true");
    } else {
      entry.button.classList.remove("active");
      entry.button.removeAttribute("aria-selected");
    }
  });
}

function clearSelectedCommand() {
  if (!ensureInsertDialogElements()) return;
  currentInsertCommand = null;
  currentInsertHandler = null;
  setActiveCommandButton(null);
  selectedCommandNameEl.textContent = "None";
  commandHintEl.textContent = "";
  configPlaceholder.hidden = false;
  configSections.hidden = true;
  clearCommandSelectionBtn.hidden = true;
  parameterContainer.innerHTML = "";
  insertDialogSubmit.disabled = true;
  clearDialogMessage();
}

function handleCommandNameChange(nextCommand) {
  if (!ensureInsertDialogElements()) return;
  if (!nextCommand || !commandDefinitions[nextCommand]) {
    return;
  }
  currentInsertCommand = nextCommand;
  selectedCommandNameEl.textContent = nextCommand;
  const meta = commandDefinitions[nextCommand];
  if (meta) {
    commandHintEl.textContent = meta.description || "";
  }
  setActiveCommandButton(nextCommand);
}

function selectCommand(commandName) {
  if (!ensureInsertDialogElements()) {
    return;
  }
  if (!commandDefinitions[commandName]) {
    setDialogMessage(`Unsupported command: ${commandName}`);
    return;
  }
  clearDialogMessage();
  currentInsertCommand = commandName;
  setActiveCommandButton(commandName);
  selectedCommandNameEl.textContent = commandName;
  clearCommandSelectionBtn.hidden = false;
  configPlaceholder.hidden = true;
  configSections.hidden = false;

  const meta = commandDefinitions[commandName];
  commandHintEl.textContent = meta?.description || "";
  parameterContainer.innerHTML = "";
  currentInsertHandler = meta.builder({
    commandName,
    container: parameterContainer,
    setCommandName: handleCommandNameChange,
  });
  insertDialogSubmit.disabled = false;
  if (currentInsertHandler && typeof currentInsertHandler.focus === "function") {
    currentInsertHandler.focus();
  }
}

function openInsertDialog(initialCommand = null) {
  if (!ensureInsertDialogElements()) {
    setStatus("Insert dialog is unavailable in this environment.");
    return;
  }
  initializeInsertDialog();
  insertCategorySelect.value = "All";
  insertSearchInput.value = "";
  applyInsertFilters();
  clearSelectedCommand();
  if (!insertDialog.open) {
    if (typeof insertDialog.showModal === "function") {
      insertDialog.showModal();
    } else {
      insertDialog.setAttribute("open", "true");
    }
  }
  if (initialCommand && commandDefinitions[initialCommand]) {
    selectCommand(initialCommand);
  }
}

function clearDialogMessage() {
  if (!ensureInsertDialogElements()) return;
  insertDialogMessage.textContent = "";
  insertDialogMessage.hidden = true;
}

function setDialogMessage(message) {
  if (!ensureInsertDialogElements()) return;
  if (!message) {
    clearDialogMessage();
    return;
  }
  insertDialogMessage.textContent = message;
  insertDialogMessage.hidden = false;
}

function getProtocolLines() {
  return protocolInput.value.split(/\r?\n/);
}

function computeRandomizeOpenCount(lines) {
  let open = 0;
  lines.forEach((line) => {
    const trimmed = line.trim().toUpperCase();
    if (!trimmed || trimmed.startsWith("//")) return;
    if (trimmed === "RANDOMIZE_ON") {
      open += 1;
    } else if (trimmed === "RANDOMIZE_OFF") {
      if (open > 0) open -= 1;
    }
  });
  return open;
}

function canInsertRandomizeOn(lines) {
  return computeRandomizeOpenCount(lines) === 0;
}

function canInsertRandomizeOff(lines) {
  return computeRandomizeOpenCount(lines) > 0;
}

function normalizeMediaValue(value) {
  if (!value) return "";
  return value.trim().replace(/^<+/, "").replace(/>+$/, "").trim();
}

function hasAllowedExtension(value, allowedExtensions) {
  if (!value) return false;
  const filePart = value.split(",")[0].trim();
  const dotIndex = filePart.lastIndexOf(".");
  if (dotIndex === -1) return false;
  const ext = filePart.slice(dotIndex + 1).toLowerCase();
  return allowedExtensions.includes(ext);
}

function appendMediaPlaceholders(base, allowMedia, soundValue, videoValue, htmlValue) {
  if (!allowMedia) return base;
  let result = base.trimEnd();
  const addPlaceholder = (value) => {
    if (!value) return;
    const placeholder = `<${value}>`;
    if (!result.includes(placeholder)) {
      result = result ? `${result} ${placeholder}` : placeholder;
    }
  };
  addPlaceholder(soundValue);
  addPlaceholder(videoValue);
  addPlaceholder(htmlValue);
  return result.trim();
}

function validateMediaValues(commandName, mediaValues) {
  const { sound, video, html } = mediaValues;
  if (!sound && !video && !html) return;
  if (!commandsWithMedia.has(commandName)) {
    throw new Error("This command does not support media attachments.");
  }
  if (sound && !hasAllowedExtension(sound, ["mp3", "wav"])) {
    throw new Error("Sound attachments must end with .mp3 or .wav.");
  }
  if (video && !hasAllowedExtension(video, ["mp4"])) {
    throw new Error("Video attachments must end with .mp4.");
  }
  if (html && !hasAllowedExtension(html, ["html"])) {
    throw new Error("HTML attachments must end with .html.");
  }
}

function applyHoldToken(value, holdSelected) {
  const holdPattern = /\s*\[HOLD]/gi;
  const sanitized = (value || "").replace(holdPattern, "").trim();
  if (holdSelected) {
    if (!sanitized) return "[HOLD]";
    return `${sanitized}[HOLD]`;
  }
  return sanitized;
}

let uniqueFieldCounter = 0;

function uniqueFieldId(prefix = "field") {
  uniqueFieldCounter += 1;
  return `${prefix}_${uniqueFieldCounter}`;
}

function createSection(title, helperText) {
  const section = document.createElement("div");
  section.className = "form-section";
  if (title) {
    const heading = document.createElement("h4");
    heading.textContent = title;
    section.append(heading);
  }
  if (helperText) {
    const helper = document.createElement("p");
    helper.className = "helper-text";
    helper.textContent = helperText;
    section.append(helper);
  }
  return section;
}

function createInfoSection(message) {
  const section = document.createElement("div");
  section.className = "form-section";
  const paragraph = document.createElement("p");
  paragraph.className = "helper-text";
  paragraph.textContent = message;
  section.append(paragraph);
  return section;
}

function createInputField(options) {
  const {
    label,
    type = "text",
    placeholder = "",
    defaultValue = "",
    helper = "",
    options: selectOptions = [],
    attributes = {},
  } = options;
  const wrapper = document.createElement("div");
  wrapper.className = "field";
  const fieldId = uniqueFieldId();
  const labelEl = document.createElement("label");
  labelEl.setAttribute("for", fieldId);
  labelEl.textContent = label;
  wrapper.append(labelEl);

  let input;
  if (type === "textarea") {
    input = document.createElement("textarea");
  } else if (type === "select") {
    input = document.createElement("select");
    selectOptions.forEach((opt) => {
      const optionEl = document.createElement("option");
      optionEl.value = opt.value;
      optionEl.textContent = opt.label;
      input.append(optionEl);
    });
  } else {
    input = document.createElement("input");
    input.type = type;
  }
  input.id = fieldId;
  if (placeholder && type !== "select") {
    input.placeholder = placeholder;
  }
  if (defaultValue !== undefined && defaultValue !== null) {
    input.value = defaultValue;
  }
  Object.entries(attributes).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      input.setAttribute(key, value);
    }
  });
  wrapper.append(input);
  if (helper) {
    const helperEl = document.createElement("p");
    helperEl.className = "helper-text";
    helperEl.textContent = helper;
    wrapper.append(helperEl);
  }
  return {
    root: wrapper,
    input,
    focus: () => input.focus(),
    getValue: () => (type === "select" ? input.value : input.value.trim()),
    setValue: (value) => {
      input.value = value ?? "";
    },
  };
}

function createCheckboxField(options) {
  const { label, helper = "", defaultChecked = false } = options;
  const wrapper = document.createElement("div");
  wrapper.className = "field";
  const labelEl = document.createElement("label");
  labelEl.className = "checkbox";
  const input = document.createElement("input");
  input.type = "checkbox";
  input.checked = defaultChecked;
  const textSpan = document.createElement("span");
  textSpan.textContent = label;
  labelEl.append(input, textSpan);
  wrapper.append(labelEl);
  if (helper) {
    const helperEl = document.createElement("p");
    helperEl.className = "helper-text";
    helperEl.textContent = helper;
    wrapper.append(helperEl);
  }
  return {
    root: wrapper,
    input,
    isChecked: () => input.checked,
    setChecked: (value) => {
      input.checked = Boolean(value);
    },
  };
}

function createRepeatableList(options) {
  const {
    title,
    helper = "",
    addLabel = "Add item",
    placeholderPrimary = "",
    placeholderSecondary = "",
    secondaryLabel,
    initialValues = [],
    minItems = 1,
    primaryLabelFactory = (index) => `Item ${index + 1}`,
    secondaryLabelFactory = (index) =>
      secondaryLabel ? `${secondaryLabel} ${index + 1}` : `Label ${index + 1}`,
  } = options;

  const section = createSection(title, helper);
  const list = document.createElement("div");
  list.className = "repeatable-list";
  section.append(list);

  const entries = [];

  function updateLabels() {
    entries.forEach((entry, index) => {
      const primaryLabelEl = entry.primaryField.root.querySelector("label");
      if (primaryLabelEl) {
        primaryLabelEl.textContent = primaryLabelFactory(index);
      }
      if (entry.secondaryField) {
        const secondaryLabelEl = entry.secondaryField.root.querySelector("label");
        if (secondaryLabelEl) {
          secondaryLabelEl.textContent = secondaryLabelFactory(index);
        }
      }
    });
  }

  function removeEntry(entry) {
    const index = entries.indexOf(entry);
    if (index !== -1) {
      entries.splice(index, 1);
      list.removeChild(entry.row);
      if (entries.length < minItems) {
        addEntry();
      }
      updateLabels();
    }
  }

  function addEntry(primary = "", secondary = "", focus = false) {
    const entry = {
      row: document.createElement("div"),
    };
    entry.row.className = secondaryLabel ? "repeat-row two-column" : "repeat-row";

    entry.primaryField = createInputField({
      label: primaryLabelFactory(entries.length),
      placeholder: placeholderPrimary,
      defaultValue: primary,
    });
    entry.row.append(entry.primaryField.root);

    if (secondaryLabel) {
      entry.secondaryField = createInputField({
        label: secondaryLabelFactory(entries.length),
        placeholder: placeholderSecondary,
        defaultValue: secondary,
      });
      entry.row.append(entry.secondaryField.root);
    }

    entry.removeBtn = document.createElement("button");
    entry.removeBtn.type = "button";
    entry.removeBtn.className = "icon-button";
    entry.removeBtn.setAttribute("aria-label", "Remove entry");
    entry.removeBtn.textContent = "✕";
    entry.removeBtn.addEventListener("click", () => removeEntry(entry));
    entry.row.append(entry.removeBtn);

    entries.push(entry);
    list.append(entry.row);
    updateLabels();
    if (focus) {
      entry.primaryField.focus();
    }
    return entry;
  }

  const defaults = initialValues.length
    ? initialValues
    : Array.from({ length: minItems }, () => ({ primary: "", secondary: "" }));
  defaults.forEach((value, index) => {
    const primaryValue = value?.primary ?? value ?? "";
    const secondaryValue = value?.secondary ?? "";
    addEntry(primaryValue, secondaryValue, index === defaults.length - 1);
  });

  const addButton = document.createElement("button");
  addButton.type = "button";
  addButton.className = "outlined-button";
  addButton.textContent = addLabel;
  addButton.addEventListener("click", () => addEntry("", "", true));
  section.append(addButton);

  return {
    root: section,
    focus: () => entries[0]?.primaryField?.focus(),
    getValues: () =>
      entries.map(({ primaryField, secondaryField }) => ({
        primary: primaryField.getValue(),
        secondary: secondaryField ? secondaryField.getValue() : "",
      })),
    setValues: (values) => {
      entries.slice().forEach(removeEntry);
      if (values && values.length) {
        values.forEach((value, index) => {
          const primaryValue = value?.primary ?? value ?? "";
          const secondaryValue = value?.secondary ?? "";
          addEntry(primaryValue, secondaryValue, index === values.length - 1);
        });
      } else {
        addEntry();
      }
      updateLabels();
    },
  };
}

function createMediaSection() {
  const section = createSection("Media attachments", "Optional references to assets in the protocol resources folder.");
  const soundField = createInputField({
    label: "Sound (mp3/wav)",
    placeholder: "audio/example.mp3",
  });
  const videoField = createInputField({
    label: "Video (mp4)",
    placeholder: "media/example.mp4",
  });
  const htmlField = createInputField({
    label: "HTML (html)",
    placeholder: "content/example.html",
  });
  section.append(soundField.root, videoField.root, htmlField.root);
  return {
    root: section,
    focus: () => soundField.focus(),
    getValues: () => ({
      sound: normalizeMediaValue(soundField.getValue()),
      video: normalizeMediaValue(videoField.getValue()),
      html: normalizeMediaValue(htmlField.getValue()),
    }),
    setValues: ({ sound = "", video = "", html = "" } = {}) => {
      soundField.setValue(sound);
      videoField.setValue(video);
      htmlField.setValue(html);
    },
  };
}

function createContinueSection(options) {
  const {
    allowHold = false,
    defaultText = "Continue",
    helper = "",
  } = options;
  const section = createSection("Continue button", helper);
  const textField = createInputField({
    label: "Button text",
    placeholder: "Continue",
    defaultValue: defaultText,
  });
  section.append(textField.root);
  let holdField = null;
  if (allowHold) {
    holdField = createCheckboxField({
      label: "Require hold to continue",
      helper: "Adds a [HOLD] token to the continue value.",
    });
    section.append(holdField.root);
  }
  return {
    root: section,
    focus: () => textField.focus(),
    getValue: () => ({
      text: textField.getValue(),
      hold: holdField ? holdField.isChecked() : false,
    }),
    setValue: ({ text, hold }) => {
      if (text !== undefined) textField.setValue(text);
      if (holdField && hold !== undefined) holdField.setChecked(hold);
    },
  };
}

function buildInstructionHandler(context) {
  const { commandName, container } = context;
  const contentSection = createSection("Instruction content");
  const headerField = createInputField({
    label: "Header text",
    placeholder: "Instruction header",
  });
  const bodyField = createInputField({
    label: "Body text",
    type: "textarea",
    placeholder: "Explain the instruction",
  });
  contentSection.append(headerField.root, bodyField.root);
  container.append(contentSection);

  const continueSection = createContinueSection({
    allowHold: holdSupportedCommands.has(commandName),
    helper: "Leave blank to use the default Continue label.",
  });
  container.append(continueSection.root);

  const mediaSection = commandsWithMedia.has(commandName) ? createMediaSection() : null;
  if (mediaSection) {
    container.append(mediaSection.root);
  }

  return {
    focus: () => headerField.focus(),
    compose: ({ command, lines }) => {
      const header = headerField.getValue() || "Header";
      const mediaValues = mediaSection ? mediaSection.getValues() : { sound: "", video: "", html: "" };
      validateMediaValues(command, mediaValues);
      const mediaProvided = Boolean(mediaValues.sound || mediaValues.video || mediaValues.html);
      let body = bodyField.getValue();
      if (!body) {
        body = mediaProvided ? "" : "Body";
      }
      const bodyWithMedia = appendMediaPlaceholders(body, commandsWithMedia.has(command), mediaValues.sound, mediaValues.video, mediaValues.html) || "Body";
      const continueValue = continueSection.getValue();
      const continueText = applyHoldToken(continueValue.text || "Continue", continueValue.hold) || "Continue";
      return `${command};${header};${bodyWithMedia};${continueText}`;
    },
  };
}

function buildTimerHandler(context) {
  const { commandName, container } = context;
  const contentSection = createSection("Timer content");
  const headerField = createInputField({
    label: "Header text",
    placeholder: "Timer header",
  });
  const bodyField = createInputField({
    label: "Body text",
    type: "textarea",
    placeholder: "Explain what happens during the timer",
  });
  const secondsField = createInputField({
    label: "Duration (seconds)",
    type: "number",
    defaultValue: "60",
    attributes: { min: "0", step: "1" },
  });
  contentSection.append(headerField.root, bodyField.root, secondsField.root);
  container.append(contentSection);

  const continueSection = createContinueSection({
    allowHold: holdSupportedCommands.has(commandName),
    helper: "Shown after the timer finishes.",
  });
  container.append(continueSection.root);

  const mediaSection = commandsWithMedia.has(commandName) ? createMediaSection() : null;
  if (mediaSection) {
    container.append(mediaSection.root);
  }

  return {
    focus: () => headerField.focus(),
    compose: ({ command }) => {
      const header = headerField.getValue() || "Header";
      const mediaValues = mediaSection ? mediaSection.getValues() : { sound: "", video: "", html: "" };
      validateMediaValues(command, mediaValues);
      const mediaProvided = Boolean(mediaValues.sound || mediaValues.video || mediaValues.html);
      let body = bodyField.getValue();
      if (!body) {
        body = mediaProvided ? "" : "Body";
      }
      const bodyWithMedia = appendMediaPlaceholders(body, commandsWithMedia.has(command), mediaValues.sound, mediaValues.video, mediaValues.html) || "Body";
      const durationRaw = secondsField.getValue();
      const duration = Number.parseInt(durationRaw, 10);
      if (!Number.isFinite(duration) || duration < 0) {
        throw new Error("Timer duration must be a non-negative integer.");
      }
      const continueValue = continueSection.getValue();
      const continueText = applyHoldToken(continueValue.text || "Continue", continueValue.hold) || "Continue";
      return `${command};${header};${bodyWithMedia};${duration};${continueText}`;
    },
  };
}

function buildScaleHandler(context) {
  const { commandName, container, setCommandName, initialRandomized = false } = context;
  const baseCommand = commandName.includes("[RANDOMIZED]") ? randomizablePairs[commandName] || "SCALE" : commandName;
  const randomizedCommand = randomizablePairs[baseCommand] || baseCommand;
  let isRandomized = initialRandomized || commandName === randomizedCommand && randomizedCommand !== baseCommand;

  const contentSection = createSection("Scale content");
  const headerField = createInputField({
    label: "Header text",
    placeholder: "Scale header",
  });
  const bodyField = createInputField({
    label: "Body text",
    type: "textarea",
    placeholder: "Prompt text shown above the responses",
  });
  contentSection.append(headerField.root, bodyField.root);
  container.append(contentSection);

  const itemsList = createRepeatableList({
    title: "Scale items",
    helper: "Define each item that should appear before the responses.",
    addLabel: "Add item",
    placeholderPrimary: "Item",
    initialValues: [{ primary: "Item 1" }],
  });
  container.append(itemsList.root);

  const responsesList = createRepeatableList({
    title: "Response options",
    helper: "Optional branching labels are wrapped in square brackets when provided.",
    addLabel: "Add response",
    placeholderPrimary: "Response text",
    placeholderSecondary: "Branch label (optional)",
    secondaryLabel: "Branch label",
    initialValues: [{ primary: "Response 1" }, { primary: "Response 2" }],
  });
  container.append(responsesList.root);

  const mediaSection = commandsWithMedia.has(baseCommand) ? createMediaSection() : null;
  if (mediaSection) {
    container.append(mediaSection.root);
  }

  if (randomizablePairs[baseCommand]) {
    const randomSection = createSection("Randomization", "Toggle to use SCALE[RANDOMIZED].");
    const randomCheckbox = createCheckboxField({
      label: "Randomize item order",
      defaultChecked: isRandomized,
    });
    randomSection.append(randomCheckbox.root);
    container.append(randomSection);
    randomCheckbox.input.addEventListener("change", () => {
      isRandomized = randomCheckbox.isChecked();
      const nextCommand = isRandomized ? randomizedCommand : baseCommand;
      setCommandName(nextCommand);
    });
  }

  return {
    focus: () => headerField.focus(),
    compose: ({ command }) => {
      const expectedCommand = isRandomized ? randomizedCommand : baseCommand;
      if (command !== expectedCommand) {
        setCommandName(expectedCommand);
      }
      const finalCommand = expectedCommand;
      const header = headerField.getValue() || "Header";
      const mediaValues = mediaSection ? mediaSection.getValues() : { sound: "", video: "", html: "" };
      validateMediaValues(finalCommand, mediaValues);
      const mediaProvided = Boolean(mediaValues.sound || mediaValues.video || mediaValues.html);
      let body = bodyField.getValue();
      if (!body) {
        body = mediaProvided ? "" : "Body";
      }
      const bodyWithMedia = appendMediaPlaceholders(body, commandsWithMedia.has(finalCommand), mediaValues.sound, mediaValues.video, mediaValues.html) || "Body";

      const itemValues = itemsList
        .getValues()
        .map((entry) => entry.primary.trim())
        .filter((value) => value.length > 0);
      const itemsPart = itemValues.length > 1 ? `[${itemValues.join(";")}]` : (itemValues[0] || "Item 1");

      const responseValues = responsesList
        .getValues()
        .map(({ primary, secondary }) => {
          const text = primary.trim();
          if (!text) return null;
          const label = secondary.trim();
          return label ? `${text} [${label}]` : text;
        })
        .filter(Boolean);
      const responsesPart = responseValues.length ? responseValues.join(";") : "Response 1;Response 2";

      return `${finalCommand};${header};${bodyWithMedia};${itemsPart};${responsesPart}`;
    },
  };
}

function buildInputFieldHandler(context) {
  const { commandName, container, setCommandName, initialRandomized = false } = context;
  const baseCommand = commandName.includes("[RANDOMIZED]") ? randomizablePairs[commandName] || "INPUTFIELD" : commandName;
  const randomizedCommand = randomizablePairs[baseCommand] || baseCommand;
  let isRandomized = initialRandomized || commandName === randomizedCommand && randomizedCommand !== baseCommand;

  const contentSection = createSection("Prompt content");
  const headerField = createInputField({
    label: "Header text",
    placeholder: "Input prompt header",
  });
  const bodyField = createInputField({
    label: "Body text",
    type: "textarea",
    placeholder: "Provide instruction for the fields",
  });
  contentSection.append(headerField.root, bodyField.root);
  container.append(contentSection);

  const fieldsList = createRepeatableList({
    title: "Input fields",
    helper: "Each entry becomes an input option. Randomization shuffles their order.",
    addLabel: "Add field",
    placeholderPrimary: "Field label",
    initialValues: [{ primary: "field1" }, { primary: "field2" }],
  });
  container.append(fieldsList.root);

  const continueSection = createContinueSection({
    allowHold: holdSupportedCommands.has(baseCommand),
    helper: "Shown after all fields are filled.",
  });
  container.append(continueSection.root);

  const mediaSection = commandsWithMedia.has(baseCommand) ? createMediaSection() : null;
  if (mediaSection) {
    container.append(mediaSection.root);
  }

  if (randomizablePairs[baseCommand]) {
    const randomSection = createSection("Randomization", "Toggle to use INPUTFIELD[RANDOMIZED].");
    const randomCheckbox = createCheckboxField({
      label: "Randomize field order",
      defaultChecked: isRandomized,
    });
    randomSection.append(randomCheckbox.root);
    container.append(randomSection);
    randomCheckbox.input.addEventListener("change", () => {
      isRandomized = randomCheckbox.isChecked();
      const nextCommand = isRandomized ? randomizedCommand : baseCommand;
      setCommandName(nextCommand);
    });
  }

  return {
    focus: () => headerField.focus(),
    compose: ({ command }) => {
      const expectedCommand = isRandomized ? randomizedCommand : baseCommand;
      if (command !== expectedCommand) {
        setCommandName(expectedCommand);
      }
      const finalCommand = expectedCommand;
      const header = headerField.getValue() || "Header";
      const mediaValues = mediaSection ? mediaSection.getValues() : { sound: "", video: "", html: "" };
      validateMediaValues(finalCommand, mediaValues);
      const mediaProvided = Boolean(mediaValues.sound || mediaValues.video || mediaValues.html);
      let body = bodyField.getValue();
      if (!body) {
        body = mediaProvided ? "" : "Body";
      }
      const bodyWithMedia = appendMediaPlaceholders(body, commandsWithMedia.has(finalCommand), mediaValues.sound, mediaValues.video, mediaValues.html) || "Body";

      const fieldValues = fieldsList
        .getValues()
        .map((entry) => entry.primary.trim())
        .filter((value) => value.length > 0);
      const fieldsPart = (fieldValues.length ? fieldValues : ["field1", "field2"]).join(";");

      const continueValue = continueSection.getValue();
      const continueText = applyHoldToken(continueValue.text || "Continue", continueValue.hold) || "Continue";

      return `${finalCommand};${header};${bodyWithMedia};${fieldsPart};${continueText}`;
    },
  };
}

function buildLabelHandler(context) {
  const { commandName, container } = context;
  const section = createSection("Label", "Labels allow GOTO commands to jump to specific lines.");
  const labelField = createInputField({
    label: "Label name",
    placeholder: "MyLabel",
  });
  section.append(labelField.root);
  container.append(section);
  return {
    focus: () => labelField.focus(),
    compose: ({ command }) => {
      const value = labelField.getValue();
      if (!value) {
        throw new Error("Label name is required.");
      }
      return `${command};${value}`;
    },
  };
}

function buildGotoHandler(context) {
  const { commandName, container } = context;
  const section = createSection("Goto", "Jump to a label defined elsewhere in the protocol.");
  const labelField = createInputField({
    label: "Target label",
    placeholder: "MyLabel",
  });
  section.append(labelField.root);
  container.append(section);
  return {
    focus: () => labelField.focus(),
    compose: ({ command }) => {
      const value = labelField.getValue();
      if (!value) {
        throw new Error("Target label is required.");
      }
      return `${command};${value}`;
    },
  };
}

function buildHtmlHandler(context) {
  const { commandName, container } = context;
  const section = createSection("HTML asset", "Provide the file name of the HTML asset located in the resources folder.");
  const filenameField = createInputField({
    label: "Filename",
    placeholder: "content/page.html",
  });
  section.append(filenameField.root);
  container.append(section);

  const continueSection = createContinueSection({
    allowHold: holdSupportedCommands.has(commandName),
    helper: "Optional. Leave blank to omit the continue step.",
  });
  container.append(continueSection.root);

  return {
    focus: () => filenameField.focus(),
    compose: ({ command }) => {
      const filename = filenameField.getValue();
      if (!filename) {
        throw new Error("Filename is required.");
      }
      const continueValue = continueSection.getValue();
      const sanitizedText = continueValue.text.trim();
      const includeContinue = continueValue.hold || sanitizedText.length > 0;
      if (includeContinue) {
        const continueText = applyHoldToken(sanitizedText || "Continue", continueValue.hold) || "Continue";
        return `${command};${filename};${continueText}`;
      }
      return `${command};${filename}`;
    },
  };
}

function buildTimerSoundHandler(context) {
  const { container } = context;
  const section = createSection("Timer sound", "Provide the sound file to play when the timer finishes.");
  const filenameField = createInputField({
    label: "Filename",
    placeholder: "audio/alarm.mp3",
  });
  section.append(filenameField.root);
  container.append(section);
  return {
    focus: () => filenameField.focus(),
    compose: ({ command }) => {
      const filename = filenameField.getValue();
      if (!filename) {
        throw new Error("Filename is required.");
      }
      return `${command};${filename}`;
    },
  };
}

function buildLogHandler(context) {
  const { container } = context;
  const section = createSection("Log", "Write a message to the session log for debugging.");
  const messageField = createInputField({
    label: "Message",
    placeholder: "Message to log",
    defaultValue: "Log message",
  });
  section.append(messageField.root);
  container.append(section);
  return {
    focus: () => messageField.focus(),
    compose: ({ command }) => {
      const value = messageField.getValue() || "Log message";
      return `${command};${value}`;
    },
  };
}

function buildEndHandler(context) {
  const { container } = context;
  container.append(createInfoSection("This command ends the protocol. No additional configuration is required."));
  return {
    focus: () => {},
    compose: () => "END",
  };
}

function buildRandomizeOnHandler() {
  return {
    focus: () => {},
    compose: ({ command, lines }) => {
      if (!canInsertRandomizeOn(lines)) {
        throw new Error("A randomization block is already open. Close it before starting a new one.");
      }
      return command;
    },
  };
}

function buildRandomizeOffHandler() {
  return {
    focus: () => {},
    compose: ({ command, lines }) => {
      if (!canInsertRandomizeOff(lines)) {
        throw new Error("Cannot close RANDOMIZE_OFF because no block is currently open.");
      }
      return command;
    },
  };
}

function buildStudyIdHandler(context) {
  const { container } = context;
  const section = createSection("Study ID", "This value must be unique within the protocol.");
  const studyField = createInputField({
    label: "Study ID",
    placeholder: "PO123",
  });
  section.append(studyField.root);
  container.append(section);
  return {
    focus: () => studyField.focus(),
    compose: ({ command, lines }) => {
      const value = studyField.getValue();
      if (!value) {
        throw new Error("Study ID is required.");
      }
      const existing = lines.some((line) => line.trim().toUpperCase().startsWith("STUDY_ID;"));
      if (existing) {
        throw new Error("A STUDY_ID already exists in this protocol.");
      }
      return `${command};${value}`;
    },
  };
}

function buildTransitionsHandler(context) {
  const { container } = context;
  const section = createSection("Transitions", "Select the transition animation for upcoming slides.");
  const transitionsField = createInputField({
    label: "Transition",
    type: "select",
    options: [
      { value: "off", label: "off" },
      { value: "slide", label: "slide" },
      { value: "slideleft", label: "slideleft" },
      { value: "fade", label: "fade" },
      { value: "dissolve", label: "dissolve" },
    ],
  });
  section.append(transitionsField.root);
  container.append(section);
  return {
    focus: () => transitionsField.focus(),
    compose: ({ command }) => {
      const value = transitionsField.getValue();
      if (!value) {
        throw new Error("Transition value is required.");
      }
      return `${command};${value}`;
    },
  };
}

function buildColorHandler(context) {
  const { commandName, container } = context;
  const section = createSection("Color", "Use #RRGGBB or #AARRGGBB format.");
  const colorField = createInputField({
    label: "Color value",
    placeholder: "#FFFFFF",
  });
  section.append(colorField.root);
  container.append(section);
  return {
    focus: () => colorField.focus(),
    compose: ({ command }) => {
      const value = colorField.getValue();
      if (!/^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(value)) {
        throw new Error("Enter a valid hex color such as #RRGGBB.");
      }
      return `${command};${value}`;
    },
  };
}

function buildSizeHandler(context) {
  const { commandName, container } = context;
  const section = createSection("Size", "Enter a positive number representing points.");
  const sizeField = createInputField({
    label: "Size",
    type: "number",
    attributes: { min: "1", step: "1" },
  });
  section.append(sizeField.root);
  container.append(section);
  return {
    focus: () => sizeField.focus(),
    compose: ({ command }) => {
      const raw = sizeField.getValue();
      const value = Number.parseInt(raw, 10);
      if (!Number.isFinite(value) || value <= 0) {
        throw new Error("Size must be a positive number.");
      }
      return `${command};${value}`;
    },
  };
}

function buildAlignmentHandler(context) {
  const { commandName, container } = context;
  const section = createSection("Alignment", "Choose LEFT, CENTER, or RIGHT.");
  const alignmentField = createInputField({
    label: "Alignment",
    type: "select",
    options: [
      { value: "LEFT", label: "LEFT" },
      { value: "CENTER", label: "CENTER" },
      { value: "RIGHT", label: "RIGHT" },
    ],
  });
  section.append(alignmentField.root);
  container.append(section);
  return {
    focus: () => alignmentField.focus(),
    compose: ({ command }) => {
      const value = alignmentField.getValue();
      if (!value) {
        throw new Error("Alignment is required.");
      }
      return `${command};${value}`;
    },
  };
}

function handleInsertFormSubmit(event) {
  event.preventDefault();
  if (!ensureInsertDialogElements()) {
    setStatus("Insert dialog is unavailable in this environment.");
    return;
  }
  if (!currentInsertCommand || !currentInsertHandler) {
    setDialogMessage("Select a command before inserting.");
    return;
  }
  try {
    const line = currentInsertHandler.compose({
      command: currentInsertCommand,
      lines: getProtocolLines(),
    });
    insertAtCursor(line);
    insertDialog.close("confirm");
    setStatus(`Inserted ${currentInsertCommand}`);
  } catch (error) {
    const message = error?.message || String(error);
    setDialogMessage(message);
  }
}

async function handleNew() {
  if (isDirty && !confirm("Discard unsaved changes?")) {
    return;
  }
  protocolInput.value = "";
  currentFileName = "Untitled";
  currentFileHandle = null;
  updateLineCount();
  validationResults = [];
  renderValidationResults();
  markSaved("Started a blank protocol");
}

async function handleLoad() {
  if (isDirty && !confirm("Discard unsaved changes?")) {
    return;
  }
  if (window.showOpenFilePicker) {
    try {
      const [handle] = await window.showOpenFilePicker({
        types: [
          {
            description: "Protocol files",
            accept: { "text/plain": [".txt"] },
          },
        ],
        excludeAcceptAllOption: false,
        multiple: false,
      });
      currentFileHandle = handle;
      const file = await handle.getFile();
      const text = await file.text();
      protocolInput.value = text;
      currentFileName = file.name;
      updateLineCount();
      validationResults = validateProtocol(text);
      renderValidationResults();
      markSaved(`Loaded ${file.name}`);
      return;
    } catch (error) {
      if (error?.name === "AbortError") {
        return;
      }
      console.warn("File picker failed, falling back", error);
    }
  }
  fileInput.value = "";
  fileInput.click();
}

async function handleFileInputChange(event) {
  const [file] = event.target.files;
  if (!file) return;
  const text = await file.text();
  protocolInput.value = text;
  currentFileHandle = null;
  currentFileName = file.name;
  updateLineCount();
  validationResults = validateProtocol(text);
  renderValidationResults();
  markSaved(`Loaded ${file.name}`);
}

async function handleSave() {
  try {
    if (currentFileHandle && window.showSaveFilePicker) {
      const writable = await currentFileHandle.createWritable();
      await writable.write(protocolInput.value);
      await writable.close();
      markSaved(`Saved to ${currentFileName}`);
    } else if (window.showSaveFilePicker) {
      const handle = await window.showSaveFilePicker({
        suggestedName: currentFileName || "protocol.txt",
        types: [
          {
            description: "Protocol files",
            accept: { "text/plain": [".txt"] },
          },
        ],
      });
      currentFileHandle = handle;
      currentFileName = handle.name;
      const writable = await handle.createWritable();
      await writable.write(protocolInput.value);
      await writable.close();
      markSaved(`Saved to ${currentFileName}`);
    } else {
      downloadProtocol();
    }
  } catch (error) {
    if (error?.name === "AbortError") return;
    console.error("Error saving file", error);
    setStatus(`Save failed: ${error.message || error}`);
  }
}

function downloadProtocol() {
  const blob = new Blob([protocolInput.value], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = currentFileName.endsWith(".txt") ? currentFileName : `${currentFileName}.txt`;
  document.body.append(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  markSaved("Downloaded current protocol");
}

function setupEventListeners() {
  protocolInput.addEventListener("input", () => {
    updateLineCount();
    markDirty();
    if (autoValidateToggle.checked) {
      debouncedValidate();
    }
  });

  validateButton.addEventListener("click", () => {
    triggerValidation();
  });

  autoValidateToggle.addEventListener("change", () => {
    if (autoValidateToggle.checked) {
      debouncedValidate();
    }
  });

  issueFilterInput.addEventListener("input", () => {
    renderValidationResults();
  });

  prevIssueButton.addEventListener("click", () => goToRelativeIssue(-1));
  nextIssueButton.addEventListener("click", () => goToRelativeIssue(1));

  newButton.addEventListener("click", handleNew);
  loadButton.addEventListener("click", handleLoad);
  saveButton.addEventListener("click", handleSave);
  downloadButton.addEventListener("click", downloadProtocol);
  fileInput.addEventListener("change", handleFileInputChange);

  if (insertButton) {
    insertButton.addEventListener("click", () => openInsertDialog());
  }
  if (insertForm) {
    insertForm.addEventListener("submit", handleInsertFormSubmit);
  }
  if (insertDialog) {
    insertDialog.addEventListener("close", () => {
      clearDialogMessage();
      clearSelectedCommand();
    });
  }

  toggleCommandsButton.addEventListener("click", () => {
    const expanded = toggleCommandsButton.getAttribute("aria-expanded") === "true";
    const nextState = !expanded;
    toggleCommandsButton.setAttribute("aria-expanded", String(nextState));
    commandsBody.hidden = !nextState;
    toggleCommandsButton.textContent = nextState ? "Hide" : "Show all";
  });

  window.addEventListener("beforeunload", (event) => {
    if (isDirty) {
      event.preventDefault();
      event.returnValue = "";
    }
  });
}

function init() {
  updateLineCount();
  populateCommandsList();
  renderValidationResults();
  setupEventListeners();
  setStatus("Ready");
}

init();
