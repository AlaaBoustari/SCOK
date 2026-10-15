const SCOK = (() => {
    let sessionReady;

    function saveSession(auth) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.setItem("userId", auth.userId);
        localStorage.setItem("username", auth.username);
    }

    function clearSession() {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");
        localStorage.removeItem("username");
    }

    function requireSession() {
        if (!sessionReady) {
            sessionReady = fetch("/api/auth/me", {cache: "no-store"})
                .then(async response => {
                    if (response.ok) {
                        saveSession(await response.json());
                    } else if (response.status !== 401 || !await refreshAccessToken()) {
                        throw new Error("Sign in again");
                    }
                    return true;
                })
                .catch(() => {
                    clearSession();
                    location.replace("/login.html");
                    return false;
                });
        }
        return sessionReady;
    }

    async function readResponse(response) {
        if (response.status === 204) return null;

        const text = await response.text();
        if (!text) return null;

        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }

    async function csrfHeaders() {
        const response = await fetch("/api/auth/csrf", {cache: "no-store"});
        if (!response.ok) throw new Error("Could not obtain CSRF token");
        const csrf = await response.json();
        return {[csrf.headerName]: csrf.token};
    }

    async function refreshAccessToken() {
        const response = await fetch("/api/auth/refresh", {
            method: "POST",
            headers: await csrfHeaders()
        });

        if (!response.ok || response.redirected) return false;
        saveSession(await response.json());
        return true;
    }

    async function api(path, options = {}, retry = true) {
        if (!await requireSession()) throw new Error("Sign in again");
        const headers = new Headers(options.headers || {});
        const method = (options.method || "GET").toUpperCase();
        if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
            const csrf = await csrfHeaders();
            for (const [name, value] of Object.entries(csrf)) headers.set(name, value);
        }
        if (options.body) headers.set("Content-Type", "application/json");

        const response = await fetch(path, {...options, headers});

        if (response.status === 401 && retry && await refreshAccessToken()) {
            return api(path, options, false);
        }

        const body = await readResponse(response);
        if (!response.ok) {
            if (response.status === 401) {
                clearSession();
                location.replace("/login.html");
            }
            const message = body?.message || body?.error || `Request failed (${response.status})`;
            throw new Error(message);
        }
        return body;
    }

    async function logout() {
        try {
            const response = await fetch("/api/auth/logout", {
                method: "POST", headers: await csrfHeaders()
            });
            if (!response.ok) alert("Local logout completed, but Cognito revocation may have failed.");
        } finally {
            clearSession();
            location.replace("/login.html");
        }
    }

    return {
        api,
        clearSession,
        logout,
        requireSession,
        refreshAccessToken,
        csrfHeaders,
        saveSession,
        userId: () => localStorage.getItem("userId"),
        username: () => localStorage.getItem("username")
    };
})();
