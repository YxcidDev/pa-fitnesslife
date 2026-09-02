'use strict';

const DISPLAY_DURATION      = 4000;
const FRONTEND_DEDUP_MS     = 3000;
const QR_MIN_LENGTH         = 4;
const API_ENDPOINT          = '/api/attendance/verify';

const qrInput           = document.getElementById('qrInput');
const stateIdle         = document.getElementById('stateIdle');
const stateProcessing   = document.getElementById('stateProcessing');
const stateResult       = document.getElementById('stateResult');

const resultCard        = document.getElementById('resultCard');
const resultTopbar      = document.getElementById('resultTopbar');
const resultAvatarRing  = document.getElementById('resultAvatarRing');
const resultPhoto       = document.getElementById('resultPhoto');
const resultPhotoFb     = document.getElementById('resultPhotoFallback');
const resultBadge       = document.getElementById('resultBadge');
const resultBadgeIcon   = document.getElementById('resultBadgeIcon');
const resultName        = document.getElementById('resultName');
const resultPlan        = document.getElementById('resultPlan');
const resultStatusBar   = document.getElementById('resultStatusBar');
const resultStatusText  = document.getElementById('resultStatusText');
const resultFooterId    = document.getElementById('resultFooterId');
const resultFooterTime  = document.getElementById('resultFooterTime');

const countdownBar      = document.getElementById('countdownBar');
const countdownFill     = document.getElementById('countdownFill');
const kioskToast        = document.getElementById('kioskToast');

const clockTime         = document.getElementById('currentTime');
const clockDate         = document.getElementById('currentDate');

let qrBuffer            = '';
let isProcessing        = false;
let returnTimer         = null;
let lastScannedCode     = null;
let lastScannedTime     = 0;

const STATUS_CONFIG = {
    ALLOWED: {
        cardClass:   'card--allowed',
        ringClass:   'ring--allowed',
        badgeClass:  'badge--allowed',
        barClass:    'bar--allowed',
        badgeIcon:   'bi-check-lg',
        topbarColor: 'var(--green)',
        fillColor:   'var(--green)',
    },
    DENIED: {
        cardClass:   'card--denied',
        ringClass:   'ring--denied',
        badgeClass:  'badge--denied',
        barClass:    'bar--denied',
        badgeIcon:   'bi-x-lg',
        topbarColor: 'var(--red)',
        fillColor:   'var(--red)',
    },
    EXIT: {
        cardClass:   'card--exit',
        ringClass:   'ring--exit',
        badgeClass:  'badge--exit',
        barClass:    'bar--exit',
        badgeIcon:   'bi-box-arrow-right',
        topbarColor: 'var(--blue)',
        fillColor:   'var(--blue)',
    },
    NOT_FOUND: {
        cardClass:   'card--denied',
        ringClass:   'ring--denied',
        badgeClass:  'badge--denied',
        barClass:    'bar--denied',
        badgeIcon:   'bi-person-x',
        topbarColor: 'var(--red)',
        fillColor:   'var(--red)',
    },
};

function updateClock() {
    const now = new Date();

    const h  = String(now.getHours()).padStart(2, '0');
    const m  = String(now.getMinutes()).padStart(2, '0');
    clockTime.textContent = `${h}:${m}`;

    const days   = ['DOM', 'LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB'];
    const months = ['ENE','FEB','MAR','ABR','MAY','JUN','JUL','AGO','SEP','OCT','NOV','DIC'];
    const d = now.getDate();
    const dayName   = days[now.getDay()];
    const monthName = months[now.getMonth()];
    const year      = now.getFullYear();
    clockDate.textContent = `${dayName} ${d} ${monthName} ${year}`;
}

setInterval(updateClock, 1000);
updateClock();

function refocusInput() {
    if (document.activeElement !== qrInput) {
        qrInput.focus();
    }
}

document.addEventListener('click',     refocusInput);
document.addEventListener('keydown',   refocusInput);
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) refocusInput();
});

window.addEventListener('load', () => {
    setTimeout(refocusInput, 100);
});

qrInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        const code = qrBuffer.trim();
        qrBuffer = '';
        qrInput.value = '';

        if (code.length < QR_MIN_LENGTH) {
            return;
        }

        handleQrCode(code);
        return;
    }

    if (e.key.length === 1) {
        qrBuffer += e.key;
    }
});

qrInput.addEventListener('input', () => {
    const val = qrInput.value;
    if (val.includes('\n') || val.includes('\r')) {
        const code = val.replace(/[\r\n]/g, '').trim();
        qrInput.value = '';
        qrBuffer = '';
        if (code.length >= QR_MIN_LENGTH) {
            handleQrCode(code);
        }
    }
});

async function handleQrCode(code) {
    if (isProcessing) return;

    const now = Date.now();
    if (code === lastScannedCode && (now - lastScannedTime) < FRONTEND_DEDUP_MS) {
        showToast('Escaneo reciente detectado, espera un momento.');
        return;
    }

    if (!isValidQrFormat(code)) {
        showToast('Formato de código QR no reconocido.');
        return;
    }

    isProcessing = true;
    lastScannedCode = code;
    lastScannedTime = now;

    if (returnTimer) {
        clearTimeout(returnTimer);
        returnTimer = null;
    }

    showState('processing');

    try {
        const response = await fetch(API_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ qrCode: code }),
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        renderResult(data);
        showState('result');
        startCountdown();

        returnTimer = setTimeout(() => {
            resetToIdle();
        }, DISPLAY_DURATION);

    } catch (err) {
        console.error('[Attendance] Error verifying QR:', err);
        showToast('Error de comunicación. Inténtalo de nuevo.');
        resetToIdle();
    } finally {
        isProcessing = false;
    }
}

function isValidQrFormat(code) {
    return typeof code === 'string'
        && code.length >= QR_MIN_LENGTH
        && code.length <= 200
        && /^[a-zA-Z0-9\-_@.]+$/.test(code);
}

function renderResult(data) {
    const status = data.status || 'NOT_FOUND';
    const cfg    = STATUS_CONFIG[status] || STATUS_CONFIG['DENIED'];

    resultCard.className       = 'result-card';
    resultAvatarRing.className = 'result-avatar-ring';
    resultBadge.className      = 'result-status-badge';
    resultStatusBar.className  = 'result-status-bar';

    resultCard.classList.add(cfg.cardClass);
    resultAvatarRing.classList.add(cfg.ringClass);
    resultBadge.classList.add(cfg.badgeClass);
    resultStatusBar.classList.add(cfg.barClass);

    resultTopbar.style.background = cfg.topbarColor;

    resultBadgeIcon.className = `bi ${cfg.badgeIcon}`;

    countdownFill.style.background = cfg.fillColor;

    resultName.textContent = data.userName || 'Usuario';

    resultPlan.textContent = data.userPlan || 'Sin plan';

    resultStatusText.textContent = data.message || '—';

    const photoUrl = data.userPhoto;
    if (photoUrl && photoUrl.trim() !== '') {
        resultPhoto.src = photoUrl;
        resultPhoto.style.display = 'block';
        resultPhotoFb.style.display = 'none';
        resultPhoto.onerror = () => {
            resultPhoto.style.display = 'none';
            resultPhotoFb.style.display = 'flex';
            renderPhotoFallback(data.userName);
        };
    } else {
        resultPhoto.style.display = 'none';
        resultPhotoFb.style.display = 'flex';
        renderPhotoFallback(data.userName);
    }

    if (data.userId) {
        resultFooterId.textContent = `ID: ${formatUserId(data.userId)}`;
    } else {
        resultFooterId.textContent = '';
    }

    const now  = new Date();
    const time = now.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase();
    const date = now.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' }).toUpperCase();
    resultFooterTime.textContent = `${date} ${time}`;
}

function renderPhotoFallback(name) {
    const initials = name
        ? name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase()
        : '?';
    resultPhotoFb.textContent = initials;
}

function formatUserId(mongoId) {
    if (!mongoId) return '';
    const suffix = mongoId.slice(-5).toUpperCase();
    const year   = new Date().getFullYear().toString().slice(-2);
    return `FL${year}-C${suffix}`;
}

function showState(name) {
    stateIdle.classList.remove('state--active');
    stateProcessing.classList.remove('state--active');
    stateResult.classList.remove('state--active');

    if (name === 'idle')       stateIdle.classList.add('state--active');
    if (name === 'processing') stateProcessing.classList.add('state--active');
    if (name === 'result')     stateResult.classList.add('state--active');
}

function resetToIdle() {
    hideCountdown();
    showState('idle');
    qrBuffer = '';
    qrInput.value = '';
    refocusInput();
}

function startCountdown() {
    countdownBar.classList.add('bar--visible');
    countdownFill.style.transition = 'none';
    countdownFill.style.transform  = 'scaleX(1)';

    void countdownFill.offsetWidth;

    countdownFill.style.transition = `transform ${DISPLAY_DURATION}ms linear`;
    countdownFill.style.transform  = 'scaleX(0)';
}

function hideCountdown() {
    countdownBar.classList.remove('bar--visible');
    countdownFill.style.transition = 'none';
    countdownFill.style.transform  = 'scaleX(1)';
}

let toastTimer = null;

function showToast(message) {
    kioskToast.textContent = message;
    kioskToast.classList.add('toast--visible');

    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        kioskToast.classList.remove('toast--visible');
    }, 2500);
}