(function () {
    "use strict";

    const root = document.getElementById("flChat");
    if (!root) return;

    const endpoint = root.dataset.endpoint || "/api/chatbot";

    const els = {
        panel: document.getElementById("flChatPanel"),
        toggle: document.getElementById("flChatToggle"),
        badge: document.getElementById("flChatBadge"),
        messages: document.getElementById("flChatMessages"),
        suggestions: document.getElementById("flChatSuggestions"),
        form: document.getElementById("flChatForm"),
        input: document.getElementById("flChatInput"),
        send: document.getElementById("flChatSend"),
        clear: document.getElementById("flChatClear"),
        close: document.getElementById("flChatClose"),
    };

    const BOT_NAME = "Asistente";
    const USER_NAME = "Tú";
    const GREETING =
        "¡Hola! Soy el asistente virtual de Fitness Life. ¿En qué puedo ayudarte hoy?";
    const SEEN_KEY = "flChatSeen";
    const MAX_INPUT_HEIGHT = 120;

    const state = { open: false, loaded: false, sending: false, lastDay: null };

    const timeFmt = new Intl.DateTimeFormat("es-CO", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
    });
    const dayFmt = new Intl.DateTimeFormat("es-CO", {
        weekday: "long",
        day: "numeric",
        month: "long",
    });

    function safeStorage(action) {
        try {
            return action();
        } catch (_) {
            return null;
        }
    }

    function dayLabel(date) {
        const today = new Date();
        const yesterday = new Date();
        yesterday.setDate(today.getDate() - 1);
        if (date.toDateString() === today.toDateString()) return "Hoy";
        if (date.toDateString() === yesterday.toDateString()) return "Ayer";
        return dayFmt.format(date);
    }

    function escapeHtml(text) {
        return text.replace(/[&<>"']/g, (c) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
        })[c]);
    }

    function renderRich(text) {
        const inline = (s) => s.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
        const lines = escapeHtml(text).split(/\r?\n/);
        let html = "";
        let list = null;

        const closeList = () => {
            if (list) {
                html += "</" + list + ">";
                list = null;
            }
        };

        for (const raw of lines) {
            const line = raw.trim();
            const ul = line.match(/^[-*•]\s+(.*)$/);
            const ol = line.match(/^\d+[.)]\s+(.*)$/);

            if (ul || ol) {
                const type = ul ? "ul" : "ol";
                if (list !== type) {
                    closeList();
                    html += "<" + type + ">";
                    list = type;
                }
                html += "<li>" + inline((ul || ol)[1]) + "</li>";
            } else if (!line) {
                closeList();
            } else {
                closeList();
                html += "<p>" + inline(line) + "</p>";
            }
        }
        closeList();
        return html;
    }

    function scrollToBottom() {
        requestAnimationFrame(() => {
            els.messages.scrollTop = els.messages.scrollHeight;
        });
    }

    function addDayDividerIfNeeded(date) {
        const key = date.toDateString();
        if (state.lastDay === key) return;
        state.lastDay = key;
        const divider = document.createElement("div");
        divider.className = "fl-day";
        divider.textContent = dayLabel(date);
        els.messages.appendChild(divider);
    }

    function buildMessage({ role, date, error }) {
        const wrapper = document.createElement("div");
        wrapper.className =
            "fl-msg fl-msg--" + (role === "user" ? "user" : "bot") + (error ? " fl-msg--error" : "");

        const meta = document.createElement("div");
        meta.className = "fl-msg__meta";

        const author = document.createElement("span");
        author.className = "fl-msg__author";
        author.textContent = role === "user" ? USER_NAME : BOT_NAME;

        const time = document.createElement("time");
        time.className = "fl-msg__time";
        time.dateTime = date.toISOString();
        time.textContent = timeFmt.format(date);

        meta.append(author, time);

        const bubble = document.createElement("div");
        bubble.className = "fl-msg__bubble";

        wrapper.append(meta, bubble);
        return { wrapper, bubble };
    }

    function addMessage({ role, text, date, error }) {
        addDayDividerIfNeeded(date);
        const { wrapper, bubble } = buildMessage({ role, date, error });

        if (role === "user") {
            bubble.textContent = text;
        } else {
            bubble.innerHTML = renderRich(text);
        }

        els.messages.appendChild(wrapper);
        scrollToBottom();
        return wrapper;
    }

    function showTyping() {
        const { wrapper, bubble } = buildMessage({ role: "bot", date: new Date() });
        wrapper.querySelector(".fl-msg__time").textContent = "escribiendo…";
        bubble.innerHTML =
            '<span class="fl-typing" aria-label="El asistente está escribiendo"><span></span><span></span><span></span></span>';
        els.messages.appendChild(wrapper);
        scrollToBottom();
        return wrapper;
    }

    function showGreeting() {
        addMessage({ role: "bot", text: GREETING, date: new Date() });
        els.suggestions.hidden = false;
    }

    async function request(path, { method = "GET", body } = {}) {
        const headers = {
            Accept: "application/json",
            "X-Requested-With": "XMLHttpRequest",
        };
        if (body !== undefined) headers["Content-Type"] = "application/json";

        const res = await fetch(endpoint + path, {
            method,
            headers,
            body,
            credentials: "same-origin",
        });

        if (res.redirected && /\/login/.test(res.url)) {
            throw { code: "session" };
        }

        if (res.status === 204) return null;

        const isJson = (res.headers.get("content-type") || "").includes("application/json");
        const data = isJson ? await res.json().catch(() => null) : null;

        if (!res.ok) {
            throw { code: res.status, message: data && data.message };
        }
        if (data === null) throw { code: "format" };
        return data;
    }

    function errorText(err) {
        if (err && err.code === "session") {
            return "Tu sesión expiró. Recarga la página e inicia sesión de nuevo para continuar.";
        }
        if (err && (err.code === 400 || err.code === 429 || err.code === 503) && err.message) {
            return err.message;
        }
        if (err && (err.code === 401 || err.code === 403)) {
            return "No tienes permiso para usar el asistente. Recarga la página e inicia sesión.";
        }
        return "No fue posible obtener una respuesta. Revisa tu conexión e inténtalo de nuevo.";
    }

    async function loadHistory() {
        try {
            const items = await request("/history");
            if (Array.isArray(items) && items.length > 0) {
                els.suggestions.hidden = true;
                items.forEach((m) =>
                    addMessage({
                        role: m.role,
                        text: m.content,
                        date: m.timestamp ? new Date(m.timestamp) : new Date(),
                    })
                );
                return;
            }
        } catch (err) {
            console.warn("Chatbot: no se pudo cargar el historial", err);
        }
        showGreeting();
    }

    function setSending(value) {
        state.sending = value;
        els.send.disabled = value;
        els.input.setAttribute("aria-busy", String(value));
    }

    async function sendMessage(rawText) {
        const text = rawText.trim();
        if (!text || state.sending) return;

        setSending(true);
        els.suggestions.hidden = true;
        addMessage({ role: "user", text, date: new Date() });
        els.input.value = "";
        autosize();

        const typing = showTyping();
        try {
            const data = await request("/message", {
                method: "POST",
                body: JSON.stringify({ message: text }),
            });
            typing.remove();
            addMessage({ role: "bot", text: data.answer, date: new Date() });
        } catch (err) {
            typing.remove();
            addMessage({ role: "bot", text: errorText(err), date: new Date(), error: true });
        } finally {
            setSending(false);
            els.input.focus();
        }
    }

    async function clearConversation() {
        if (state.sending) return;
        if (!window.confirm("¿Borrar toda la conversación?")) return;
        try {
            await request("/history", { method: "DELETE" });
        } catch (err) {
            addMessage({ role: "bot", text: errorText(err), date: new Date(), error: true });
            return;
        }
        els.messages.replaceChildren();
        state.lastDay = null;
        showGreeting();
    }

    function openChat() {
        state.open = true;
        els.panel.hidden = false;
        requestAnimationFrame(() => root.classList.add("is-open"));
        els.toggle.setAttribute("aria-expanded", "true");
        els.toggle.setAttribute("aria-label", "Cerrar asistente virtual");
        els.badge.hidden = true;
        safeStorage(() => sessionStorage.setItem(SEEN_KEY, "1"));
        document.documentElement.classList.add("fl-chat-lock");

        if (!state.loaded) {
            state.loaded = true;
            loadHistory();
        } else {
            scrollToBottom();
        }
        els.input.focus({ preventScroll: true });
    }

    function closeChat(returnFocus = true) {
        state.open = false;
        root.classList.remove("is-open");
        els.toggle.setAttribute("aria-expanded", "false");
        els.toggle.setAttribute("aria-label", "Abrir asistente virtual");
        document.documentElement.classList.remove("fl-chat-lock");
        setTimeout(() => {
            if (!state.open) els.panel.hidden = true;
        }, 200);
        if (returnFocus) els.toggle.focus();
    }

    function autosize() {
        els.input.style.height = "auto";
        els.input.style.height = Math.min(els.input.scrollHeight, MAX_INPUT_HEIGHT) + "px";
    }

    els.toggle.addEventListener("click", () => (state.open ? closeChat() : openChat()));
    els.close.addEventListener("click", () => closeChat());
    els.clear.addEventListener("click", clearConversation);

    els.form.addEventListener("submit", (e) => {
        e.preventDefault();
        sendMessage(els.input.value);
    });

    els.input.addEventListener("input", autosize);
    els.input.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey && !e.isComposing) {
            e.preventDefault();
            els.form.requestSubmit();
        }
    });

    els.suggestions.addEventListener("click", (e) => {
        const chip = e.target.closest(".fl-chat__chip");
        if (chip) sendMessage(chip.dataset.text || chip.textContent);
    });

    root.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && state.open) closeChat();
    });

    if (safeStorage(() => sessionStorage.getItem(SEEN_KEY)) === "1") {
        els.badge.hidden = true;
    }
})();
