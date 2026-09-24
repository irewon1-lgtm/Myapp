"use strict";
const STORAGE_KEY = "study-log-v1";
const MAX_MINUTES = 600;
const MAX_RECORDS = 2000;
const form = document.querySelector("#record-form");
const topicInput = document.querySelector("#topic");
const minutesInput = document.querySelector("#minutes");
const addButton = document.querySelector("#add-record");
const message = document.querySelector("#message");
const list = document.querySelector("#records");
const filter = document.querySelector("#filter");
const summary = document.querySelector("#summary");
const empty = document.querySelector("#empty");
let records = [];
let writable = true;
let loadingTopics = false;
minutesInput.max = String(MAX_MINUTES);

function announce(text) {
  message.textContent = text;
}

function parseInput(topicText, minutesText) {
  const topic = topicText.trim();
  const raw = minutesText.trim();
  const minutes = Number(raw);
  if (topic === "" || topic.length > 80) {
    return { ok: false, error: "공부한 내용을 1~80자 범위로 적어 주세요." };
  }
  if (raw === "" || !Number.isInteger(minutes)
      || minutes < 1 || minutes > MAX_MINUTES) {
    return { ok: false, error: "시간은 1~" + MAX_MINUTES + "분의 정수로 적어 주세요." };
  }
  return { ok: true, topic: topic, minutes: minutes };
}

function validData(data) {
  if (data === null || typeof data !== "object" || Array.isArray(data)) return false;
  if (data.version !== 1 || !Array.isArray(data.records)) return false;
  if (data.records.length > MAX_RECORDS) return false;
  const ids = new Set();
  for (const record of data.records) {
    if (record === null || typeof record !== "object" || Array.isArray(record)) return false;
    if (!Number.isSafeInteger(record.id) || record.id < 1 || ids.has(record.id)) return false;
    if (typeof record.topic !== "string" || record.topic.trim() === "" || record.topic.length > 80) return false;
    if (!Number.isInteger(record.minutes) || record.minutes < 1 || record.minutes > MAX_MINUTES) return false;
    if (typeof record.completed !== "boolean") return false;
    ids.add(record.id);
  }
  return true;
}

function loadRecords() {
  try {
    const text = localStorage.getItem(STORAGE_KEY);
    if (text === null) {
      records = [];
      return;
    }
    const data = JSON.parse(text);
    if (!validData(data)) throw new Error("지원하지 않는 기록 형식");
    records = data.records;
  } catch (error) {
    writable = false;
    announce("기존 저장 자료를 읽지 못했습니다. 원본은 덮어쓰지 않았습니다. 내보내기로 보존한 뒤 확인해 주세요.");
  }
}

function commitRecords(nextRecords, successText) {
  if (!writable) {
    announce("기존 자료 확인이 필요하여 변경을 잠갔습니다.");
    return false;
  }
  const data = { version: 1, records: nextRecords };
  if (!validData(data)) {
    announce("기록 형식이나 개수 제한을 확인해 주세요.");
    return false;
  }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    announce("저장하지 못했습니다. 기존 기록과 입력값은 유지했습니다.");
    return false;
  }
  records = nextRecords;
  renderRecords();
  announce(successText);
  return true;
}

function nextRecordId() {
  let maximum = 0;
  for (const record of records) {
    if (record.id > maximum) maximum = record.id;
  }
  return maximum + 1;
}

function visibleRecord(record) {
  if (filter.value === "active") return !record.completed;
  if (filter.value === "completed") return record.completed;
  return true;
}

function renderRecords() {
  list.replaceChildren();
  let total = 0;
  let visibleCount = 0;
  for (const record of records) {
    total = total + record.minutes;
    if (!visibleRecord(record)) continue;
    visibleCount = visibleCount + 1;
    const item = document.createElement("li");
    item.className = "record";
    const text = document.createElement("p");
    const stateText = record.completed ? "완료" : "진행 중";
    text.textContent = record.topic + " · " + record.minutes + "분 · " + stateText;
    const actions = document.createElement("div");
    actions.className = "actions";
    const toggle = document.createElement("button");
    toggle.type = "button";
    toggle.textContent = record.completed ? "완료 취소" : "완료 표시";
    toggle.setAttribute("aria-pressed", String(record.completed));
    toggle.addEventListener("click", function () {
      toggleRecord(record.id);
    });
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "삭제";
    remove.setAttribute("aria-label", record.topic + " 기록 삭제");
    remove.addEventListener("click", function () {
      deleteRecord(record.id);
    });
    actions.append(toggle, remove);
    item.append(text, actions);
    list.append(item);
  }
  summary.textContent = "전체 " + records.length + "개 · 총 " + total + "분 · 현재 표시 " + visibleCount + "개";
  empty.textContent = visibleCount === 0 ? "이 조건에 해당하는 기록이 없습니다." : "";
  addButton.disabled = !writable;
}

function handleSubmit(event) {
  event.preventDefault();
  const input = parseInput(topicInput.value, minutesInput.value);
  if (!input.ok) {
    announce(input.error);
    return;
  }
  const next = records.slice();
  next.push({ id: nextRecordId(), topic: input.topic, minutes: input.minutes, completed: false });
  if (commitRecords(next, "기록을 이 브라우저에 저장했습니다.")) {
    form.reset();
    topicInput.focus();
  }
}

function toggleRecord(id) {
  const next = [];
  for (const record of records) {
    next.push({
      id: record.id,
      topic: record.topic,
      minutes: record.minutes,
      completed: record.id === id ? !record.completed : record.completed
    });
  }
  if (commitRecords(next, "완료 상태를 저장했습니다.")) {
    document.querySelector("#list-title").focus();
  }
}

function deleteRecord(id) {
  const next = [];
  for (const record of records) {
    if (record.id !== id) next.push(record);
  }
  if (commitRecords(next, "기록을 삭제하고 저장했습니다.")) {
    document.querySelector("#list-title").focus();
  }
}

function exportRecords() {
  let text;
  try {
    text = localStorage.getItem(STORAGE_KEY);
  } catch (error) {
    announce("저장소에 접근하지 못해 내보낼 수 없습니다.");
    return;
  }
  if (text === null) {
    announce("내보낼 저장 자료가 없습니다.");
    return;
  }
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "study-log-backup.txt";
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  announce("저장 자료의 다운로드를 요청했습니다. 파일이 실제로 저장됐는지 확인해 주세요.");
}

function resetRecords() {
  const accepted = window.confirm("이 브라우저의 학습 기록을 모두 초기화합니다. 필요한 자료를 내보냈는지 확인하세요. 계속할까요?");
  if (!accepted) return;
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    announce("초기화하지 못했습니다. 기존 자료는 유지합니다.");
    return;
  }
  writable = true;
  records = [];
  renderRecords();
  announce("이 기록장의 저장 자료만 초기화했습니다.");
}

function validTopics(data) {
  if (!Array.isArray(data) || data.length > 100) return false;
  for (const item of data) {
    if (item === null || typeof item !== "object") return false;
    if (typeof item.id !== "string" || typeof item.title !== "string") return false;
    if (item.id.trim() === "" || item.title.trim() === "" || item.title.length > 120) return false;
  }
  return true;
}

async function loadTopics() {
  if (loadingTopics) return;
  loadingTopics = true;
  const button = document.querySelector("#load-topics");
  const status = document.querySelector("#topics-message");
  const topicList = document.querySelector("#topics-list");
  button.disabled = true;
  status.textContent = "주제를 불러오는 중입니다.";
  const controller = new AbortController();
  const timer = setTimeout(function () { controller.abort(); }, 5000);
  try {
    const response = await fetch("./topics.json", { signal: controller.signal });
    if (!response.ok) throw new Error("HTTP " + response.status);
    const data = await response.json();
    if (!validTopics(data)) throw new Error("주제 형식 오류");
    topicList.replaceChildren();
    for (const topic of data) {
      const item = document.createElement("li");
      item.textContent = topic.title;
      topicList.append(item);
    }
    status.textContent = data.length === 0 ? "제공되는 주제가 없습니다." : data.length + "개 주제를 불러왔습니다.";
  } catch (error) {
    status.textContent = error.name === "AbortError"
      ? "요청 시간이 길어 중단했습니다. 다시 시도해 주세요."
      : "주제를 불러오지 못했습니다. 다시 시도해 주세요.";
  } finally {
    clearTimeout(timer);
    loadingTopics = false;
    button.disabled = false;
  }
}

form.addEventListener("submit", handleSubmit);
filter.addEventListener("change", renderRecords);
document.querySelector("#export-records").addEventListener("click", exportRecords);
document.querySelector("#reset-records").addEventListener("click", resetRecords);
document.querySelector("#load-topics").addEventListener("click", loadTopics);
loadRecords();
renderRecords();
