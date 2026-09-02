document.addEventListener("DOMContentLoaded", () => {
    const modalEl      = document.getElementById("modalPasswordReset");
    const bsModal      = new bootstrap.Modal(modalEl);

    const stepEmail    = document.getElementById("stepEmail");
    const stepOtp      = document.getElementById("stepOtp");
    const stepDone     = document.getElementById("stepDone");

    const inputEmail   = document.getElementById("resetEmail");
    const btnSendOtp   = document.getElementById("btnSendOtp");
    const msgEmail     = document.getElementById("msgEmail");

    const inputOtp     = document.getElementById("resetOtp");
    const inputNewPass = document.getElementById("resetNewPass");
    const inputConfirm = document.getElementById("resetConfirmPass");
    const btnReset     = document.getElementById("btnReset");
    const msgOtp       = document.getElementById("msgOtp");
    const btnBackEmail = document.getElementById("btnBackEmail");

    let savedEmail  = "";

    document.getElementById("btnForgotPassword").addEventListener("click", (e) => {
        e.preventDefault();
        showStep("email");
        bsModal.show();
    });

    btnSendOtp.addEventListener("click", async () => {
        const email = inputEmail.value.trim();

        if (!isValidEmail(email)) {
            showMsg(msgEmail, "Ingresa un email válido.", "danger");
            return;
        }

        setLoading(btnSendOtp, true, "Enviando...");
        clearMsg(msgEmail);

        try {
            const res  = await fetch("/api/password/send-otp", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body:    `email=${encodeURIComponent(email)}`
            });
            const data = await res.json();

            if (data.ok) {
                savedEmail = email;
                showStep("otp");
                showMsg(msgOtp, "Código enviado. Revisa tu correo.", "success");
            } else {
                showMsg(msgEmail, data.msg || "No se pudo enviar el código.", "warning");
            }
        } catch (err) {
            showMsg(msgEmail, "Error de conexión. Intenta de nuevo.", "danger");
        } finally {
            setLoading(btnSendOtp, false, "Enviar código");
        }
    });

    btnReset.addEventListener("click", async () => {
        const otp        = inputOtp.value.trim();
        const newPass    = inputNewPass.value;
        const confirmPass = inputConfirm.value;

        if (otp.length !== 6 || isNaN(otp)) {
            showMsg(msgOtp, "El código debe ser de 6 dígitos.", "danger");
            return;
        }
        if (newPass.length < 6) {
            showMsg(msgOtp, "La contraseña debe tener al menos 6 caracteres.", "danger");
            return;
        }
        if (newPass !== confirmPass) {
            showMsg(msgOtp, "Las contraseñas no coinciden.", "danger");
            return;
        }

        setLoading(btnReset, true, "Verificando...");
        clearMsg(msgOtp);

        try {
            const res  = await fetch("/api/password/reset", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: `email=${encodeURIComponent(savedEmail)}`
                    + `&otp=${encodeURIComponent(otp)}`
                    + `&newPassword=${encodeURIComponent(newPass)}`
            });
            const data = await res.json();

            if (data.ok) {
                showStep("done");
            } else {
                showMsg(msgOtp, data.msg || "Error al actualizar la contraseña.", "danger");
            }
        } catch (err) {
            showMsg(msgOtp, "Error de conexión. Intenta de nuevo.", "danger");
        } finally {
            setLoading(btnReset, false, "Cambiar contraseña");
        }
    });

    btnBackEmail.addEventListener("click", () => showStep("email"));

    modalEl.addEventListener("hidden.bs.modal", resetModal);

    function showStep(step) {
        stepEmail.classList.add("d-none");
        stepOtp.classList.add("d-none");
        stepDone.classList.add("d-none");

        if (step === "email") stepEmail.classList.remove("d-none");
        if (step === "otp")   stepOtp.classList.remove("d-none");
        if (step === "done")  stepDone.classList.remove("d-none");
    }

    function showMsg(el, text, type) {
        el.textContent  = text;
        el.className    = `alert alert-${type} py-2 small mt-2`;
        el.style.display = "block";
    }

    function clearMsg(el) {
        el.textContent   = "";
        el.style.display = "none";
    }

    function setLoading(btn, loading, text) {
        btn.disabled    = loading;
        btn.textContent = loading ? text : btn.dataset.label || text;
        if (!loading) btn.dataset.label = text;
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function resetModal() {
        inputEmail.value    = "";
        inputOtp.value      = "";
        inputNewPass.value  = "";
        inputConfirm.value  = "";
        savedEmail       = "";
        clearMsg(msgEmail);
        clearMsg(msgOtp);
        showStep("email");
    }
});
