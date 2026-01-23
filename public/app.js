import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.4/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-auth.js";
import {
  getFirestore,
  collection,
  query,
  orderBy,
  limit,
  startAfter,
  getDocs,
  doc,
  getDoc,
  updateDoc,
  setDoc,
  deleteDoc,
  serverTimestamp
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyAY2zdhQD-4QYO5KoHqM8mLG8btCy9Sjig",
  authDomain: "psgameslibrary.firebaseapp.com",
  projectId: "psgameslibrary",
  storageBucket: "psgameslibrary.firebasestorage.app",
  messagingSenderId: "750390892142",
  appId: "1:750390892142:web:698ebf6c990d74642e387f"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const ADMIN_DOC_REF = doc(db, "adminAccess", "config");
const BAN_DOC_REF = doc(db, "adminAccess", "banned");
const PAGE_SIZE = 6;

const els = {
  authStatus: document.getElementById("authStatus"),
  btnSignIn: document.getElementById("btnSignIn"),
  btnSignOut: document.getElementById("btnSignOut"),
  btnSignOutAlt: document.getElementById("btnSignOutAlt"),
  loginStatus: document.getElementById("loginStatus"),
  topbar: document.getElementById("topbar"),
  adminShell: document.getElementById("adminShell"),
  loginShell: document.getElementById("loginShell"),
  banInput: document.getElementById("banInput"),
  btnBanAdd: document.getElementById("btnBanAdd"),
  banList: document.getElementById("banList"),
  banStatus: document.getElementById("banStatus"),
  accessBadge: document.getElementById("accessBadge"),
  panelTitle: document.getElementById("panelTitle"),
  listContainer: document.getElementById("listContainer"),
  listStatus: document.getElementById("listStatus"),
  btnLoadMore: document.getElementById("btnLoadMore"),
  btnRefresh: document.getElementById("btnRefresh"),
  toggleHideArchived: document.getElementById("toggleHideArchived"),
  toast: document.getElementById("toast"),
  modal: document.getElementById("modal"),
  modalTitle: document.getElementById("modalTitle"),
  modalBody: document.getElementById("modalBody"),
  modalFooter: document.getElementById("modalFooter"),
  modalClose: document.getElementById("modalClose")
};

const state = {
  user: null,
  isAdmin: false,
  adminEmails: [],
  bannedEmails: [],
  activeTab: "tests",
  hideArchived: false,
  loading: {
    tests: false,
    testComments: false,
    gameComments: false
  },
  hasMore: {
    tests: true,
    testComments: true,
    gameComments: true
  },
  lastDocs: {
    tests: null,
    testComments: null,
    gameComments: null
  },
  tests: [],
  testComments: [],
  gameComments: []
};

const TAB_LABELS = {
  tests: "Останні тести",
  testComments: "Останні коментарі тестів",
  gameComments: "Останні коментарі ігор"
};

function syncBodyState() {
  document.body.classList.toggle("has-user", !!state.user);
  document.body.classList.toggle("show-admin", !!state.user && state.isAdmin);
}

function showToast(message, tone = "info") {
  els.toast.textContent = message;
  els.toast.classList.add("show");
  els.toast.dataset.tone = tone;
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => {
    els.toast.classList.remove("show");
  }, 3200);
}

function setListStatus(message) {
  els.listStatus.textContent = message;
}

function setAccessBadge(text, variant = "info") {
  els.accessBadge.textContent = text;
  const map = {
    info: "badge",
    ok: "badge",
    warn: "badge",
    error: "badge"
  };
  els.accessBadge.className = map[variant] || "badge";
}

function setAuthUI() {
  if (!state.user) {
    els.authStatus.textContent = "Потрібен вхід.";
    els.btnSignOut.hidden = true;
    els.btnSignOutAlt.hidden = true;
    els.loginStatus.textContent = "Увійдіть через Google, щоб продовжити.";
    return;
  }

  const email = state.user.email || "без email";
  els.authStatus.textContent = `Вхід: ${email}`;
  els.btnSignOut.hidden = false;
  els.btnSignOutAlt.hidden = false;
  els.loginStatus.textContent = `Вхід: ${email}`;
}

function setAdminUI() {
  syncBodyState();

  if (!state.user) {
    setAccessBadge("Немає активного входу", "warn");
    els.listContainer.innerHTML = "";
    setListStatus("Очікування входу…");
    els.topbar.hidden = true;
    els.adminShell.hidden = true;
    els.loginShell.hidden = false;
    els.loginStatus.textContent = "Увійдіть через Google, щоб продовжити.";
    updateLoadMoreButton();
    return;
  }

  if (!state.isAdmin) {
    setAccessBadge("Доступ заборонено для цього email", "error");
    els.listContainer.innerHTML = "";
    setListStatus("Доступ обмежено. Перевірте adminAccess/config");
    els.loginStatus.textContent = "Цей email не має доступу до панелі керування.";
    els.topbar.hidden = true;
    els.adminShell.hidden = true;
    els.loginShell.hidden = false;
    updateLoadMoreButton();
    return;
  }

  setAccessBadge("Доступ підтверджено", "ok");
  els.loginStatus.textContent = "";
  els.topbar.hidden = false;
  els.adminShell.hidden = false;
  els.loginShell.hidden = true;
  syncBodyState();
}

async function loadBanList() {
  if (!state.isAdmin) return;
  try {
    const snap = await getDoc(BAN_DOC_REF);
    const data = snap.exists() ? snap.data() : {};
    const list = Array.isArray(data.bannedEmails) ? data.bannedEmails : [];
    state.bannedEmails = list;
    renderBanList();
  } catch (error) {
    console.error(error);
    els.banStatus.textContent = "Не вдалося завантажити список блокувань.";
  }
}

function renderBanList() {
  els.banList.innerHTML = "";
  if (!state.bannedEmails.length) {
    els.banStatus.textContent = "Список порожній.";
    return;
  }

  els.banStatus.textContent = `Заблоковано: ${state.bannedEmails.length}`;
  state.bannedEmails.forEach((email) => {
    const item = createEl("div", "ban-item");
    item.appendChild(createEl("span", null, email));
    const btn = createEl("button", "btn ghost", "Розблокувати");
    btn.addEventListener("click", () => removeBan(email));
    item.appendChild(btn);
    els.banList.appendChild(item);
  });
}

async function addBan() {
  const email = (els.banInput.value || "").trim().toLowerCase();
  if (!email) return;
  if (state.bannedEmails.includes(email)) {
    showToast("Email вже у списку", "error");
    return;
  }
  const next = [...state.bannedEmails, email];
  try {
    await setDoc(BAN_DOC_REF, { bannedEmails: next }, { merge: true });
    state.bannedEmails = next;
    els.banInput.value = "";
    renderBanList();
    showToast("Email заблоковано");
  } catch (error) {
    console.error(error);
    showToast("Не вдалося додати блокування", "error");
  }
}

async function removeBan(email) {
  const next = state.bannedEmails.filter((item) => item !== email);
  try {
    await setDoc(BAN_DOC_REF, { bannedEmails: next }, { merge: true });
    state.bannedEmails = next;
    renderBanList();
    showToast("Email розблоковано");
  } catch (error) {
    console.error(error);
    showToast("Не вдалося зняти блокування", "error");
  }
}

async function checkAdminAccess(user) {
  if (!user) {
    state.isAdmin = false;
    state.adminEmails = [];
    return;
  }

  try {
    const snap = await getDoc(ADMIN_DOC_REF);
    if (!snap.exists()) {
      state.isAdmin = false;
      state.adminEmails = [];
      setAccessBadge("Документ adminAccess/config не знайдено", "warn");
      return;
    }

    const data = snap.data() || {};
    const allowedEmails = Array.isArray(data.allowedEmails)
      ? data.allowedEmails
      : data.adminEmail
        ? [data.adminEmail]
        : [];

    state.adminEmails = allowedEmails;
    state.isAdmin = allowedEmails.includes(user.email);
  } catch (error) {
    console.error(error);
    state.isAdmin = false;
    state.adminEmails = [];
    setAccessBadge("Помилка перевірки доступу", "error");
  }
}

function attachTabHandlers() {
  document.querySelectorAll(".tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      const tab = btn.dataset.tab;
      if (tab === state.activeTab) return;

      document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
      btn.classList.add("active");
      state.activeTab = tab;
      els.panelTitle.textContent = TAB_LABELS[tab];
      refreshActiveTab();
    });
  });
}

function resetTabState(tab) {
  state.lastDocs[tab] = null;
  state.hasMore[tab] = true;
}

function clearTabData(tab) {
  state[tab] = [];
}

async function refreshActiveTab() {
  if (!state.isAdmin) return;
  resetTabState(state.activeTab);
  clearTabData(state.activeTab);
  await loadActiveTab(false);
}

async function loadActiveTab(loadMore) {
  if (!state.isAdmin) return;
  if (state.activeTab === "tests") {
    await loadTests(loadMore);
  } else if (state.activeTab === "testComments") {
    await loadTestComments(loadMore);
  } else if (state.activeTab === "gameComments") {
    await loadGameComments(loadMore);
  }
}

function formatDate(value) {
  if (!value) return "";
  if (typeof value.toDate === "function") {
    return value.toDate().toLocaleString("uk-UA");
  }
  if (typeof value === "number") {
    return new Date(value).toLocaleString("uk-UA");
  }
  return String(value);
}

function safeText(value) {
  if (value === null || value === undefined) return "";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function createEl(tag, className, text) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (text !== undefined) el.textContent = text;
  return el;
}

function createTag(text, variant) {
  const tag = createEl("span", "tag" + (variant ? ` ${variant}` : ""), text);
  return tag;
}

function getTestStatusVariant(status) {
  if (status === "WORKING") return "success";
  if (status === "NOT_WORKING") return "danger";
  return "warning";
}

function renderTests() {
  els.listContainer.innerHTML = "";
  const filtered = state.hideArchived
    ? state.tests.filter((item) => !item.data.archived)
    : state.tests;

  if (!filtered.length) {
    els.listContainer.appendChild(createEl("div", "muted", "Немає даних."));
    updateLoadMoreButton();
    return;
  }

  filtered.forEach((item) => {
    const card = createEl("div", "card");
    const header = createEl("div", "card-header");
    const title = createEl(
      "div",
      "card-title",
      item.data.title || `Гра: ${item.data.gameId || "без ID"}`
    );

    const statusTag = createTag(
      item.data.status || "UNTESTED",
      getTestStatusVariant(item.data.status)
    );

    const meta = createEl("div", "card-meta");
    meta.appendChild(createEl("span", null, `ID: ${item.id}`));
    if (item.data.gameId) meta.appendChild(createEl("span", null, `GameID: ${item.data.gameId}`));
    if (item.data.authorEmail) {
      meta.appendChild(createEl("span", null, `Автор: ${item.data.authorEmail}`));
    }
    const updatedAt = item.data.updatedAtMillis || item.data.updatedAt;
    if (updatedAt) meta.appendChild(createEl("span", null, `Оновлено: ${formatDate(updatedAt)}`));
    if (item.data.archived) meta.appendChild(createTag("Архів", "warning"));

    header.appendChild(title);
    header.appendChild(statusTag);

    const body = createEl("div", "card-body");
    const summary = [
      item.data.testedDeviceModel,
      item.data.testedAndroidVersion,
      item.data.testedApp,
      item.data.testedAppVersion
    ]
      .filter(Boolean)
      .join(" · ");

    body.textContent = summary || "Без короткого опису";

    const actions = createEl("div", "card-actions");
    const btnDetails = createEl("button", "btn ghost", "Деталі");
    btnDetails.addEventListener("click", () => openDetailsModal(item));

    const btnEdit = createEl("button", "btn", "Редагувати JSON");
    btnEdit.addEventListener("click", () => openJsonEditor("gameTests", item));

    const btnArchive = createEl(
      "button",
      "btn",
      item.data.archived ? "Повернути" : "В архів"
    );
    btnArchive.addEventListener("click", () => toggleArchive(item));

    const btnDelete = createEl("button", "btn danger", "Видалити");
    if (item.data.archived) {
      btnDelete.disabled = true;
      btnDelete.textContent = "В архіві";
    }
    btnDelete.addEventListener("click", () => deleteItem("gameTests", item));

    actions.append(btnDetails, btnEdit, btnArchive, btnDelete);
    card.append(header, meta, body, actions);
    els.listContainer.appendChild(card);
  });
  updateLoadMoreButton();
}

function renderComments(type) {
  els.listContainer.innerHTML = "";
  const list = state[type];
  if (!list.length) {
    els.listContainer.appendChild(createEl("div", "muted", "Немає даних."));
    updateLoadMoreButton();
    return;
  }

  list.forEach((item) => {
    const card = createEl("div", "card");
    const header = createEl("div", "card-header");
    const title = createEl(
      "div",
      "card-title",
      item.data.authorEmail || item.data.authorName || "Анонімний коментар"
    );

    const meta = createEl("div", "card-meta");
    meta.appendChild(createEl("span", null, `ID: ${item.id}`));
    if (item.data.gameId) meta.appendChild(createEl("span", null, `GameID: ${item.data.gameId}`));
    if (item.data.testId) meta.appendChild(createEl("span", null, `TestID: ${item.data.testId}`));
    if (item.data.authorEmail) {
      meta.appendChild(createEl("span", null, `Email: ${item.data.authorEmail}`));
    }
    const createdAt = item.data.createdAt || item.data.testMillis;
    if (createdAt) meta.appendChild(createEl("span", null, `Дата: ${formatDate(createdAt)}`));

    const body = createEl("div", "card-body", item.data.text || "(без тексту)");

    const actions = createEl("div", "card-actions");
    const btnEdit = createEl("button", "btn", "Редагувати текст");
    btnEdit.addEventListener("click", () => openCommentEditor(type, item));

    const btnJson = createEl("button", "btn ghost", "JSON");
    btnJson.addEventListener("click", () => openJsonEditor(type, item));

    const btnDelete = createEl("button", "btn danger", "Видалити");
    btnDelete.addEventListener("click", () => deleteItem(type, item));

    header.appendChild(title);
    actions.append(btnEdit, btnJson, btnDelete);

    card.append(header, meta, body, actions);
    els.listContainer.appendChild(card);
  });
  updateLoadMoreButton();
}

async function loadTests(loadMore) {
  if (state.loading.tests) return;
  state.loading.tests = true;
  setListStatus("Завантаження тестів…");

  try {
    let q = query(
      collection(db, "gameTests"),
      orderBy("updatedAt", "desc"),
      limit(PAGE_SIZE)
    );

    if (loadMore && state.lastDocs.tests) {
      q = query(
        collection(db, "gameTests"),
        orderBy("updatedAt", "desc"),
        startAfter(state.lastDocs.tests),
        limit(PAGE_SIZE)
      );
    }

    const snap = await getDocs(q);
    const docs = snap.docs.map((d) => ({ id: d.id, data: d.data() }));

    state.lastDocs.tests = snap.docs[snap.docs.length - 1] || state.lastDocs.tests;
    state.hasMore.tests = snap.size === PAGE_SIZE;

    state.tests = loadMore ? [...state.tests, ...docs] : docs;
    renderTests();
    setListStatus(state.hasMore.tests ? "Готово. Є ще дані." : "Готово. Це все.");
  } catch (error) {
    console.error(error);
    setListStatus("Помилка завантаження тестів.");
    showToast("Не вдалося завантажити тести", "error");
  } finally {
    state.loading.tests = false;
    updateLoadMoreButton();
  }
}

async function loadTestComments(loadMore) {
  if (state.loading.testComments) return;
  state.loading.testComments = true;
  setListStatus("Завантаження коментарів тестів…");

  try {
    let q = query(
      collection(db, "testComments"),
      orderBy("createdAt", "desc"),
      limit(PAGE_SIZE)
    );

    if (loadMore && state.lastDocs.testComments) {
      q = query(
        collection(db, "testComments"),
        orderBy("createdAt", "desc"),
        startAfter(state.lastDocs.testComments),
        limit(PAGE_SIZE)
      );
    }

    const snap = await getDocs(q);
    const docs = snap.docs.map((d) => ({ id: d.id, data: d.data() }));

    state.lastDocs.testComments =
      snap.docs[snap.docs.length - 1] || state.lastDocs.testComments;
    state.hasMore.testComments = snap.size === PAGE_SIZE;

    state.testComments = loadMore ? [...state.testComments, ...docs] : docs;
    renderComments("testComments");
    setListStatus(state.hasMore.testComments ? "Готово. Є ще дані." : "Готово. Це все.");
  } catch (error) {
    console.error(error);
    setListStatus("Помилка завантаження коментарів тестів.");
    showToast("Не вдалося завантажити коментарі", "error");
  } finally {
    state.loading.testComments = false;
    updateLoadMoreButton();
  }
}

async function loadGameComments(loadMore) {
  if (state.loading.gameComments) return;
  state.loading.gameComments = true;
  setListStatus("Завантаження коментарів ігор…");

  try {
    let q = query(
      collection(db, "gameComments"),
      orderBy("createdAt", "desc"),
      limit(PAGE_SIZE)
    );

    if (loadMore && state.lastDocs.gameComments) {
      q = query(
        collection(db, "gameComments"),
        orderBy("createdAt", "desc"),
        startAfter(state.lastDocs.gameComments),
        limit(PAGE_SIZE)
      );
    }

    const snap = await getDocs(q);
    const docs = snap.docs.map((d) => ({ id: d.id, data: d.data() }));

    state.lastDocs.gameComments =
      snap.docs[snap.docs.length - 1] || state.lastDocs.gameComments;
    state.hasMore.gameComments = snap.size === PAGE_SIZE;

    state.gameComments = loadMore ? [...state.gameComments, ...docs] : docs;
    renderComments("gameComments");
    setListStatus(state.hasMore.gameComments ? "Готово. Є ще дані." : "Готово. Це все.");
  } catch (error) {
    console.error(error);
    setListStatus("Помилка завантаження коментарів ігор.");
    showToast("Не вдалося завантажити коментарі", "error");
  } finally {
    state.loading.gameComments = false;
    updateLoadMoreButton();
  }
}

function openModal(title, bodyEl, footerButtons = []) {
  els.modalTitle.textContent = title;
  els.modalBody.innerHTML = "";
  els.modalBody.appendChild(bodyEl);
  els.modalFooter.innerHTML = "";
  footerButtons.forEach((btn) => els.modalFooter.appendChild(btn));
  els.modal.classList.add("show");
  els.modal.setAttribute("aria-hidden", "false");
}

function closeModal() {
  els.modal.classList.remove("show");
  els.modal.setAttribute("aria-hidden", "true");
}

function openDetailsModal(item) {
  const grid = createEl("div", "detail-grid");
  const entries = Object.entries(item.data || {});

  if (!entries.length) {
    grid.appendChild(createEl("div", "muted", "Документ порожній."));
  } else {
    entries.forEach(([key, value]) => {
      const row = createEl("div", "detail-row");
      row.appendChild(createEl("div", "detail-label", key));
      row.appendChild(createEl("div", "detail-value", safeText(value)));
      grid.appendChild(row);
    });
  }

  const btnClose = createEl("button", "btn ghost", "Закрити");
  btnClose.addEventListener("click", closeModal);

  openModal("Деталі тесту", grid, [btnClose]);
}

function openJsonEditor(collectionName, item) {
  const wrapper = createEl("div");
  const note = createEl(
    "div",
    "muted",
    "Увага: збереження JSON повністю перезаписує документ. Перевіряйте поля уважно."
  );
  const textarea = document.createElement("textarea");
  textarea.value = JSON.stringify(item.data || {}, null, 2);
  wrapper.append(note, textarea);

  const btnSave = createEl("button", "btn primary", "Зберегти");
  btnSave.addEventListener("click", async () => {
    try {
      const parsed = JSON.parse(textarea.value || "{}");
      const ref = doc(db, collectionName, item.id);
      await setDoc(ref, parsed, { merge: false });
      if (collectionName === "gameTests") {
        await updateDoc(ref, {
          updatedAt: serverTimestamp(),
          updatedAtMillis: Date.now()
        });
      } else if (collectionName === "testComments" || collectionName === "gameComments") {
        await updateDoc(ref, { editedAt: serverTimestamp() });
      }
      showToast("Документ оновлено");
      closeModal();
      refreshActiveTab();
    } catch (error) {
      console.error(error);
      showToast("Помилка збереження JSON", "error");
    }
  });

  const btnCancel = createEl("button", "btn ghost", "Скасувати");
  btnCancel.addEventListener("click", closeModal);

  openModal("Редагування JSON", wrapper, [btnCancel, btnSave]);
}

function openCommentEditor(collectionName, item) {
  const wrapper = createEl("div");
  const textarea = document.createElement("textarea");
  textarea.value = item.data.text || "";
  wrapper.appendChild(textarea);

  const btnSave = createEl("button", "btn primary", "Зберегти текст");
  btnSave.addEventListener("click", async () => {
    const text = textarea.value.trim();
    if (!text) {
      showToast("Текст не може бути порожнім", "error");
      return;
    }
    try {
      const ref = doc(db, collectionName, item.id);
      await updateDoc(ref, { text, editedAt: serverTimestamp() });
      showToast("Коментар оновлено");
      closeModal();
      refreshActiveTab();
    } catch (error) {
      console.error(error);
      showToast("Не вдалося оновити коментар", "error");
    }
  });

  const btnCancel = createEl("button", "btn ghost", "Скасувати");
  btnCancel.addEventListener("click", closeModal);

  openModal("Редагування коментаря", wrapper, [btnCancel, btnSave]);
}

async function toggleArchive(item) {
  const ref = doc(db, "gameTests", item.id);
  const nextArchived = !item.data.archived;
  try {
    await updateDoc(ref, {
      archived: nextArchived,
      archivedAt: nextArchived ? serverTimestamp() : null
    });
    showToast(nextArchived ? "Перенесено в архів" : "Повернуто з архіву");
    refreshActiveTab();
  } catch (error) {
    console.error(error);
    showToast("Не вдалося змінити архів", "error");
  }
}

async function deleteItem(collectionName, item) {
  if (collectionName === "gameTests" && item.data.archived) {
    showToast("Архівні тести не можна видаляти", "error");
    return;
  }
  const confirmed = window.confirm("Ви точно хочете видалити цей документ?");
  if (!confirmed) return;

  try {
    await deleteDoc(doc(db, collectionName, item.id));
    showToast("Документ видалено");
    refreshActiveTab();
  } catch (error) {
    console.error(error);
    showToast("Видалення не вдалося", "error");
  }
}

function updateLoadMoreButton() {
  const tab = state.activeTab;
  const isLoading = state.loading[tab];
  els.btnLoadMore.disabled = !state.isAdmin || !state.hasMore[tab] || isLoading;
  els.btnLoadMore.hidden = !state.isAdmin;
}

els.modalClose.addEventListener("click", closeModal);
els.modal.addEventListener("click", (event) => {
  if (event.target.classList.contains("modal-backdrop")) {
    closeModal();
  }
});

els.btnSignIn.addEventListener("click", async () => {
  try {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  } catch (error) {
    console.error(error);
    showToast("Не вдалося увійти", "error");
  }
});

els.btnSignOut.addEventListener("click", async () => {
  await signOut(auth);
});

els.btnSignOutAlt.addEventListener("click", async () => {
  await signOut(auth);
});

els.btnLoadMore.addEventListener("click", async () => {
  const tab = state.activeTab;
  if (!state.hasMore[tab]) {
    showToast("Більше немає даних");
    return;
  }
  await loadActiveTab(true);
});

els.btnRefresh.addEventListener("click", refreshActiveTab);
els.toggleHideArchived.addEventListener("change", () => {
  state.hideArchived = els.toggleHideArchived.checked;
  if (state.activeTab === "tests") renderTests();
});

attachTabHandlers();

onAuthStateChanged(auth, async (user) => {
  state.user = user;
  setAuthUI();
  await checkAdminAccess(user);
  setAdminUI();

  if (state.isAdmin) {
    await loadBanList();
    await refreshActiveTab();
  }
});

// Default state before auth resolves.
setAdminUI();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js");
  });
}

els.btnBanAdd.addEventListener("click", addBan);
els.banInput.addEventListener("keydown", (event) => {
  if (event.key === "Enter") addBan();
});
