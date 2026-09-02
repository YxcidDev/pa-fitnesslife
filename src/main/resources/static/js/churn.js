document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.btn-churn-detail').forEach(function (btn) {
        btn.addEventListener('click', function () {

            const name            = btn.getAttribute('data-name')          || '';
            const email           = btn.getAttribute('data-email')         || '';
            const plan            = btn.getAttribute('data-plan')          || '';
            const planLevel       = parseInt(btn.getAttribute('data-plan-level') || '1', 10);
            const profile         = btn.getAttribute('data-profile')       || '';
            const risk            = btn.getAttribute('data-risk')          || '';
            const prediction      = btn.getAttribute('data-prediction')    || '';
            const confidence      = parseFloat(btn.getAttribute('data-confidence') || '0');
            const daysAtt         = parseInt(btn.getAttribute('data-days-att') || '0', 10);
            const attendances     = btn.getAttribute('data-attendances')   || '0';
            const avgMin          = parseFloat(btn.getAttribute('data-avg-min') || '0').toFixed(0);
            const nPayments       = btn.getAttribute('data-n-payments')    || '0';
            const daysPayment     = parseInt(btn.getAttribute('data-days-payment') || '0', 10);
            const participates    = btn.getAttribute('data-participates') === 'true';
            const reasons         = btn.getAttribute('data-reasons')       || '';
            const recommendations = btn.getAttribute('data-recommendations') || '';

            // ── Cabecera ─────────────────────────────────────────────────────
            document.getElementById('modalUserName').textContent   = name;
            document.getElementById('modalNameDetail').textContent = name;
            document.getElementById('modalEmail').textContent      = email;
            document.getElementById('modalAvatar').src =
                'https://ui-avatars.com/api/?name=' + encodeURIComponent(name)
                + '&background=eb6608&color=fff&size=128';

            const riskBadge = document.getElementById('modalRiskBadge');
            riskBadge.className = 'churn-risk-badge fs-6';
            if (risk === 'HIGH') {
                riskBadge.classList.add('churn-risk-high');
                riskBadge.textContent = 'Riesgo alto';
            } else if (risk === 'MEDIUM') {
                riskBadge.classList.add('churn-risk-medium');
                riskBadge.textContent = 'Riesgo medio';
            } else {
                riskBadge.classList.add('churn-risk-low');
                riskBadge.textContent = 'Riesgo bajo';
            }

            document.getElementById('modalPlan').textContent = plan;

            const profileLabels = {
                'comprometido': 'Comprometido',
                'irregular':    'Irregular',
                'desmotivado':  'Desmotivado'
            };
            document.getElementById('modalProfile').textContent =
                profileLabels[profile] || profile;

            document.getElementById('modalDaysAtt').textContent =
                daysAtt >= 90 ? 'Sin registro' : daysAtt + ' días';
            document.getElementById('modalAttendances').textContent = attendances;
            document.getElementById('modalAvgMin').textContent      = avgMin + ' min';
            document.getElementById('modalNPayments').textContent   = nPayments;
            document.getElementById('modalDaysPayment').textContent =
                daysPayment === 0 ? 'Vigente' : daysPayment + ' días';
            document.getElementById('modalParticipates').textContent =
                participates ? 'Sí' : 'No';

            const predEl = document.getElementById('modalPrediction');
            predEl.textContent = prediction === 'Yes' ? 'Probable deserción' : 'Continuará activo';
            predEl.style.color = prediction === 'Yes' ? '#e74c3c' : '#2ecc71';

            const confEl  = document.getElementById('modalConfidence');
            const confBar = document.getElementById('modalConfidenceBar');
            const confPct = Math.round(confidence);

            confEl.textContent = confPct + '%';

            let barColor;
            if (confPct >= 85) {
                barColor = '#eb6608';
            } else if (confPct >= 65) {
                barColor = '#f39c12';
            } else {
                barColor = 'rgba(255,255,255,0.25)';
            }

            confBar.style.width       = '0%';
            confBar.style.background  = barColor;
            setTimeout(function () {
                confBar.style.width = confPct + '%';
            }, 80);

            const reasonsList = document.getElementById('modalReasons');
            reasonsList.innerHTML = '';
            reasons.split(',').forEach(function (r) {
                const trimmed = r.trim().replace(/[\[\]]/g, '');
                if (trimmed) {
                    const li = document.createElement('li');
                    li.textContent = trimmed;
                    reasonsList.appendChild(li);
                }
            });

            const recsList = document.getElementById('modalRecommendations');
            recsList.innerHTML = '';
            recommendations.split(',').forEach(function (r) {
                const trimmed = r.trim().replace(/[\[\]]/g, '');
                if (trimmed) {
                    const li = document.createElement('li');
                    li.textContent = trimmed;
                    recsList.appendChild(li);
                }
            });

            window._activeChurnUser = { name, email, plan, risk };

            const modal = new bootstrap.Modal(document.getElementById('churnDetailModal'));
            modal.show();
        });
    });

});

function triggerRecalculate() {
    const btn = document.getElementById('btnPredict');
    if (!btn) return;
    btn.disabled = true;
    btn.innerHTML =
        '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Procesando...';

    fetch('/api/churn/recalculate', { method: 'POST' })
        .then(function (res) { return res.json(); })
        .then(function (data) {
            const seg = (data.elapsedMs / 1000).toFixed(1);
            showToast('✓ Predicción completada: ' + data.processed + ' usuarios en ' + seg + 's');
            setTimeout(function () { window.location.reload(); }, 1500);
        })
        .catch(function () {
            showToast('Error al procesar. Intenta de nuevo.');
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-arrow-clockwise me-2"></i>Actualizar predicción';
        });
}

function contactUser() {
    const user = window._activeChurnUser;
    if (!user) return;
    const firstName = user.name.split(' ')[0];
    const subject = encodeURIComponent('Fitness Life — Te echamos de menos, ' + firstName + '!');
    const body    = encodeURIComponent(
        'Hola ' + firstName + ',\n\n' +
        'Notamos que llevas un tiempo sin visitarnos y queremos saber cómo estás.\n' +
        'En Fitness Life estamos aquí para apoyarte en tu proceso.\n\n' +
        '¿Hay algo en lo que podamos ayudarte? Escríbenos o visítanos pronto.\n\n' +
        'El equipo de Fitness Life'
    );
    window.open('mailto:' + (user.email || '') + '?subject=' + subject + '&body=' + body);
    showToast('Abriendo correo para contactar a ' + user.name);
}

function sendPromotion() {
    const user = window._activeChurnUser;
    if (!user) return;
    const firstName = user.name.split(' ')[0];
    const subject = encodeURIComponent('🎁 Oferta exclusiva para ti — Fitness Life');
    const body    = encodeURIComponent(
        'Hola ' + firstName + ',\n\n' +
        'Tenemos una oferta especial para ti: 20% de descuento en tu próxima renovación.\n' +
        'Válida por los próximos 7 días. ¡No la dejes pasar!\n\n' +
        'El equipo de Fitness Life'
    );
    window.open('mailto:' + (user.email || '') + '?subject=' + subject + '&body=' + body);
    showToast('Promoción enviada a ' + user.name);
}

function registerFollowUp() {
    const user = window._activeChurnUser;
    if (!user) return;
    showToast('Seguimiento registrado para ' + user.name
        + '. Recuerda hacer contacto en los próximos 3 días.');
}

function showToast(message) {
    const toastEl  = document.getElementById('churnToast');
    const toastMsg = document.getElementById('toastMsg');
    if (!toastEl || !toastMsg) return;
    toastMsg.textContent = message;
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
}