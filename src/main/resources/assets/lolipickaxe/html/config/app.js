(() => {
  const CATEGORY_ICONS = {
    "全部功能": "✦",
    "基础与挖掘": "◆",
    "范围与战斗": "◎",
    "玩家处置": "◇",
    "特殊打击": "△",
    "显示与辅助": "◌"
  };

  const state = {
    hydrated: false,
    options: [],
    values: {},
    defaults: {},
    activeCategory: "全部功能",
    search: "",
    dirty: false
  };

  const el = {};

  function bridge(action, payload = {}) {
    const message = {
      action,
      payload,
      nonce: `${Date.now()}-${Math.random().toString(36).slice(2)}`
    };
    location.hash = "lolibridge=" + encodeURIComponent(JSON.stringify(message));
  }

  function cacheDom() {
    el.viewport = document.getElementById("uiViewport");
    el.categoryNav = document.getElementById("categoryNav");
    el.configGrid = document.getElementById("configGrid");
    el.searchInput = document.getElementById("searchInput");
    el.viewTitle = document.getElementById("viewTitle");
    el.visibleCount = document.getElementById("visibleCount");
    el.enabledCount = document.getElementById("enabledCount");
    el.changeText = document.getElementById("changeText");
    el.changeDot = document.getElementById("changeDot");
    el.changeState = document.querySelector(".change-state");
    el.saveButton = document.getElementById("saveButton");
    el.resetButton = document.getElementById("resetButton");
    el.closeButton = document.getElementById("closeButton");
    el.toast = document.getElementById("toast");
  }

  function applyViewport(meta = {}) {
    const rawScale = Number(meta.uiScale);
    const scale = Number.isFinite(rawScale) ? Math.max(1, Math.min(6, rawScale)) : 1;
    const logicalWidth = Number(meta.viewportWidth) || (window.innerWidth / scale);
    document.documentElement.style.setProperty("--ui-scale", String(scale));
    el.viewport.classList.toggle("compact", logicalWidth <= 900);
    el.viewport.classList.toggle("mobile", logicalWidth <= 680);
  }

  function categories() {
    const set = new Set(state.options.map(o => o.category));
    return ["全部功能", ...set];
  }

  function renderCategories() {
    el.categoryNav.innerHTML = "";
    categories().forEach(category => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "category-button" + (state.activeCategory === category ? " active" : "");
      button.innerHTML = `<span class="category-icon">${CATEGORY_ICONS[category] || "•"}</span><span>${escapeHtml(category)}</span>`;
      button.addEventListener("click", () => {
        state.activeCategory = category;
        renderCategories();
        renderGrid();
      });
      el.categoryNav.appendChild(button);
    });
  }

  function filteredOptions() {
    const query = state.search.trim().toLocaleLowerCase();
    return state.options.filter(option => {
      const categoryMatch = state.activeCategory === "全部功能" || option.category === state.activeCategory;
      const searchMatch = !query
        || option.label.toLocaleLowerCase().includes(query)
        || option.key.toLocaleLowerCase().includes(query)
        || option.category.toLocaleLowerCase().includes(query);
      return categoryMatch && searchMatch;
    });
  }

  function renderGrid() {
    if (!state.hydrated) return;
    const options = filteredOptions();
    el.configGrid.innerHTML = "";
    el.viewTitle.textContent = state.search ? "搜索结果" : state.activeCategory;
    el.visibleCount.textContent = String(options.length);
    updateEnabledCount();

    if (!options.length) {
      el.configGrid.innerHTML = `<div class="empty-state">没有匹配的配置项<span>试试其它关键词或切换回“全部功能”</span></div>`;
      return;
    }

    const fragment = document.createDocumentFragment();
    options.forEach(option => fragment.appendChild(createCard(option)));
    el.configGrid.appendChild(fragment);
  }

  function createCard(option) {
    const card = document.createElement("article");
    card.className = "config-card";

    const meta = option.type === "boolean"
      ? `<span class="type-pill">toggle</span>`
      : option.type === "string"
        ? `<span class="type-pill">text</span>`
        : `<span class="type-pill">${escapeHtml(option.type)}</span><span class="range-pill">${formatNumber(option.min)} — ${formatNumber(option.max)}</span>`;

    const copy = document.createElement("div");
    copy.className = "config-copy";
    copy.innerHTML = `
      <div class="config-title-row">
        <span class="config-title">${escapeHtml(option.label)}</span>
        ${meta}
      </div>
      <div class="config-key" title="${escapeAttr(option.key)}">${escapeHtml(option.key)}</div>
    `;

    const control = document.createElement("div");
    control.className = "config-control";
    control.appendChild(createControl(option));

    card.append(copy, control);
    return card;
  }

  function createControl(option) {
    if (option.type === "boolean") {
      const label = document.createElement("label");
      label.className = "galaxy-switch";
      const input = document.createElement("input");
      input.type = "checkbox";
      input.checked = Boolean(state.values[option.key]);
      input.setAttribute("aria-label", option.label);
      const track = document.createElement("span");
      track.className = "switch-track";
      track.innerHTML = `<span class="switch-nebula"></span>`;
      input.addEventListener("change", () => {
        state.values[option.key] = input.checked;
        markDirty();
        updateEnabledCount();
      });
      label.append(input, track);
      return label;
    }

    const wrap = document.createElement("div");
    wrap.className = "field-wrap";
    const input = document.createElement("input");
    input.className = "web-input" + (option.type === "string" ? " string-input" : "");
    input.value = state.values[option.key] ?? "";
    input.setAttribute("aria-label", option.label);

    if (option.type === "string") {
      input.type = "text";
      input.maxLength = 100;
    } else {
      input.type = "number";
      input.min = String(option.min);
      input.max = String(option.max);
      input.step = option.type === "int" ? "1" : "0.01";
    }

    const commit = () => {
      if (option.type === "string") {
        state.values[option.key] = input.value.slice(0, 100);
      } else {
        let value = Number(input.value);
        if (!Number.isFinite(value)) value = Number(state.values[option.key] ?? option.defaultValue ?? 0);
        value = Math.min(option.max, Math.max(option.min, value));
        if (option.type === "int") value = Math.round(value);
        state.values[option.key] = value;
        input.value = String(value);
      }
      markDirty();
    };
    input.addEventListener("change", commit);
    input.addEventListener("blur", commit);
    wrap.appendChild(input);
    return wrap;
  }

  function markDirty() {
    state.dirty = true;
    el.changeState.classList.add("dirty");
    el.changeText.textContent = "有尚未保存的修改";
  }

  function markClean() {
    state.dirty = false;
    el.changeState.classList.remove("dirty");
    el.changeText.textContent = "配置已同步";
  }

  function updateEnabledCount() {
    const boolKeys = state.options.filter(o => o.type === "boolean").map(o => o.key);
    const count = boolKeys.reduce((sum, key) => sum + (state.values[key] ? 1 : 0), 0);
    el.enabledCount.textContent = String(count);
  }

  function resetDefaults() {
    state.values = { ...state.defaults };
    markDirty();
    renderGrid();
    showToast("已恢复为默认值，点击“保存并应用”后生效");
  }

  function save() {
    if (!state.hydrated) return;
    el.saveButton.disabled = true;
    el.saveButton.style.opacity = ".75";
    el.changeText.textContent = "正在写入配置…";
    bridge("save", { values: state.values });
  }

  let toastTimer = 0;
  function showToast(text) {
    clearTimeout(toastTimer);
    el.toast.textContent = text;
    el.toast.classList.add("show");
    toastTimer = setTimeout(() => el.toast.classList.remove("show"), 2200);
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function escapeAttr(value) { return escapeHtml(value); }

  function formatNumber(value) {
    return Number.isInteger(value) ? String(value) : String(Number(value.toFixed(4)));
  }

  function bindEvents() {
    el.searchInput.addEventListener("input", event => {
      state.search = event.target.value;
      renderGrid();
    });
    el.resetButton.addEventListener("click", resetDefaults);
    el.saveButton.addEventListener("click", save);
    el.closeButton.addEventListener("click", () => bridge("close"));

    document.addEventListener("keydown", event => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        save();
      }
    });
  }

  window.LoliConfigApp = {
    hydrate(payload) {
      applyViewport(payload);
      state.options = Array.isArray(payload.options) ? payload.options : [];
      state.values = Object.fromEntries(state.options.map(option => [option.key, option.value]));
      state.defaults = Object.fromEntries(state.options.map(option => [option.key, option.defaultValue]));
      state.hydrated = true;
      state.activeCategory = "全部功能";
      state.search = "";
      el.searchInput.value = "";
      markClean();
      renderCategories();
      renderGrid();
    },
    viewport(meta) {
      applyViewport(meta);
    }
  };

  document.addEventListener("DOMContentLoaded", () => {
    cacheDom();
    bindEvents();
    bridge("ready");
  });
})();
