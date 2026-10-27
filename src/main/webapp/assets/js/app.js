/* ExamLens UI behaviour. External file because the Content-Security-Policy forbids inline script. */
(function () {
    "use strict";

    var root = document.documentElement;
    var meta = function (name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute("content") : "";
    };
    var ctxPath = meta("app-context") || "";
    var csrf = meta("csrf-token") || "";

    /* ----------------------------------------------------- theme */
    var THEME_KEY = "examlens.theme";
    var serverTheme = root.getAttribute("data-theme") || "system";

    function storedTheme() {
        try { return window.localStorage.getItem(THEME_KEY); } catch (e) { return null; }
    }
    function applyTheme(theme) {
        root.setAttribute("data-theme", theme);
        try { window.localStorage.setItem(THEME_KEY, theme); } catch (e) { /* ignore */ }
        document.querySelectorAll("[data-theme-toggle]").forEach(function (btn) {
            btn.setAttribute("data-current-theme", theme);
        });
    }
    // Local preference wins for immediate paint; it is also pushed to the server below.
    var local = storedTheme();
    if (local && local !== serverTheme) {
        root.setAttribute("data-theme", local);
    }
    var effective = local || serverTheme;

    function persistTheme(theme) {
        if (!csrf) { return; }
        fetch(ctxPath + "/settings/theme", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded", "X-CSRF-Token": csrf },
            body: "theme=" + encodeURIComponent(theme)
        }).catch(function () { /* theme still applies locally */ });
    }

    document.addEventListener("click", function (event) {
        var btn = event.target.closest("[data-theme-toggle]");
        if (!btn) { return; }
        event.preventDefault();
        var order = ["system", "light", "dark"];
        var current = root.getAttribute("data-theme") || "system";
        var next = order[(order.indexOf(current) + 1) % order.length];
        applyTheme(next);
        persistTheme(next);
    });

    /* ----------------------------------------------------- mobile drawer */
    var sidebar = document.querySelector(".sidebar");
    var backdrop = document.querySelector(".drawer-backdrop");
    function setDrawer(open) {
        if (!sidebar) { return; }
        sidebar.classList.toggle("is-open", open);
        if (backdrop) { backdrop.classList.toggle("is-open", open); }
    }
    document.addEventListener("click", function (event) {
        if (event.target.closest("[data-drawer-open]")) { setDrawer(true); return; }
        if (event.target.closest("[data-drawer-close]") || event.target === backdrop) { setDrawer(false); }
    });

    /* ----------------------------------------------------- flash messages */
    document.querySelectorAll(".flash").forEach(function (flash) {
        var close = flash.querySelector(".flash__close");
        if (close) {
            close.addEventListener("click", function () { flash.remove(); });
        }
        if (flash.classList.contains("flash--success") || flash.classList.contains("flash--info")) {
            window.setTimeout(function () {
                flash.style.transition = "opacity .4s";
                flash.style.opacity = "0";
                window.setTimeout(function () { flash.remove(); }, 420);
            }, 6500);
        }
    });

    /* ----------------------------------------------------- confirm + submit lock */
    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) { return; }
        var message = form.getAttribute("data-confirm");
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        if (form.getAttribute("data-no-lock") === null) {
            window.setTimeout(function () {
                form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(function (b) {
                    b.disabled = true;
                    if (b.dataset.workingLabel) { b.textContent = b.dataset.workingLabel; }
                });
            }, 0);
        }
    }, true);

    /* ----------------------------------------------------- check-all */
    document.addEventListener("change", function (event) {
        var master = event.target.closest("[data-check-all]");
        if (!master) { return; }
        var form = master.closest("form") || document;
        var name = master.getAttribute("data-check-target");
        form.querySelectorAll('input[type="checkbox"]' + (name ? '[name="' + name + '"]' : "")).forEach(function (box) {
            if (box !== master) { box.checked = master.checked; }
        });
    });

    /* ----------------------------------------------------- copy buttons */
    document.addEventListener("click", function (event) {
        var btn = event.target.closest("[data-copy]");
        if (!btn) { return; }
        var target = document.getElementById(btn.getAttribute("data-copy"));
        if (!target) { return; }
        var text = target.innerText;
        var done = function () {
            var original = btn.textContent;
            btn.textContent = "Copied";
            window.setTimeout(function () { btn.textContent = original; }, 1400);
        };
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(done).catch(function () {});
        } else {
            var range = document.createRange();
            range.selectNodeContents(target);
            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(range);
            done();
        }
    });

    /* ----------------------------------------------------- flashcard reveal */
    document.addEventListener("click", function (event) {
        var btn = event.target.closest("[data-reveal]");
        if (!btn) { return; }
        event.preventDefault();
        var target = document.getElementById(btn.getAttribute("data-reveal"));
        if (!target) { return; }
        var shown = target.classList.toggle("hidden");
        btn.textContent = shown ? "Show answer" : "Hide answer";
    });

    /* ----------------------------------------------------- char counter */
    document.querySelectorAll("[data-counter-for]").forEach(function (out) {
        var field = document.getElementById(out.getAttribute("data-counter-for"));
        if (!field) { return; }
        var max = field.getAttribute("maxlength");
        var update = function () {
            out.textContent = field.value.length + (max ? " / " + max : "") + " characters";
        };
        field.addEventListener("input", update);
        update();
    });
})();
