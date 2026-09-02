(function () {
    'use strict';

    const POLL_INTERVAL_MS = 60_000;
    const ENDPOINT         = '/api/attendance/active-count';

    const badge = document.getElementById('onlineCount');
    if (!badge) return;

    function toLabel(count) {
        if (count === 0)        return 'Vacío';
        if (count <= 5)         return 'Tranquilo';
        if (count <= 12)        return 'Moderado';
        if (count <= 18)        return 'Concurrido';
        return                         'Lleno';
    }

    async function fetchCount() {
        try {
            const res  = await fetch(ENDPOINT, { credentials: 'same-origin' });
            if (!res.ok) return;
            const data = await res.json();
            badge.textContent = toLabel(data.count ?? 0);
        } catch {
        }
    }

    fetchCount();
    setInterval(fetchCount, POLL_INTERVAL_MS);
})();