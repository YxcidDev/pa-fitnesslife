document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.btnEditAccess').forEach(button => {
        button.addEventListener('click', function () {
            const accessId     = this.getAttribute('data-access-id');
            const currentPage  = this.getAttribute('data-current-page') || '0';
            const resultFilter = this.getAttribute('data-result-filter') || '';
            const searchTerm   = this.getAttribute('data-search-term') || '';

            document.getElementById('currentPageInput').value  = currentPage;
            document.getElementById('resultFilterInput').value = resultFilter;
            document.getElementById('searchTermInput').value   = searchTerm;

            const form = document.getElementById('editAccessForm');
            form.action = `/admin/accesses/update-result/${accessId}`;

            const modal = new bootstrap.Modal(document.getElementById('editAccessModal'));
            modal.show();
        });
    });

    document.querySelectorAll('.alert').forEach(alert => {
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

function viewAccessDetails(button) {
    const accessId     = button.getAttribute('data-access-id');
    const modalContent = document.getElementById('accessDetailsContent');

    modalContent.innerHTML = `
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Cargando...</span>
            </div>
        </div>
    `;

    fetch(`/admin/accesses/get/${accessId}`)
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener el registro');
            return response.json();
        })
        .then(attendance => {
            const resultBadge = getResultBadge(attendance.result);
            const duration    = calcDuration(attendance.checkIn, attendance.checkOut);

            modalContent.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Información del usuario</h6>
                        <div class="mb-2">
                            <strong>Nombre:</strong> ${attendance.userName || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Email:</strong> ${attendance.userEmail || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Plan:</strong> ${attendance.userPlan || 'Sin plan'}
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Información del acceso</h6>
                        <div class="mb-2">
                            <strong>Resultado:</strong> ${resultBadge}
                        </div>
                        <div class="mb-2">
                            <strong>Código QR:</strong> ${attendance.qrCode || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Entrada:</strong> ${formatDate(attendance.checkIn)}
                        </div>
                        <div class="mb-2">
                            <strong>Salida:</strong> ${attendance.checkOut
                                ? formatDate(attendance.checkOut)
                                : '<em class="text-muted">Sin registrar</em>'}
                        </div>
                        ${duration ? `
                        <div class="mb-2">
                            <strong>Duración:</strong> ${duration}
                        </div>` : ''}
                    </div>
                </div>

                <hr class="my-4"/>
                <div class="row">
                    <div class="col-12">
                        <h6 class="text-muted mb-3">Estado del plan</h6>
                        ${attendance.userPlan ? `
                        <div class="mb-2">
                            <strong>Plan activo:</strong>
                            <span>${attendance.userPlan}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Membresía:</strong>
                            <span class="badge bg-success">Activa</span>
                        </div>
                        ` : `
                        <div class="alert alert-warning">
                            El usuario no tiene un plan activo registrado en este acceso.
                        </div>
                        `}
                    </div>
                </div>

                <hr class="my-4"/>
                <div class="row">
                    <div class="col-md-6">
                        <small class="text-muted">
                            <strong>Registrado:</strong> ${formatDate(attendance.checkIn)}
                        </small>
                    </div>
                </div>
            `;
        })
        .catch(error => {
            console.error('Error:', error);
            modalContent.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-circle me-2"></i>
                    Error al cargar los detalles del acceso
                </div>
            `;
        });
}

function getResultBadge(result) {
    const badges = {
        'ALLOWED': '<span class="badge bg-success">Permitido</span>',
        'DENIED':  '<span class="badge bg-danger">Denegado</span>'
    };
    return badges[result] || '<span class="badge bg-secondary">Desconocido</span>';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('es-CO', {
        year:   'numeric',
        month:  '2-digit',
        day:    '2-digit',
        hour:   '2-digit',
        minute: '2-digit',
        second: '2-digit'
    }).format(date);
}

function calcDuration(checkIn, checkOut) {
    if (!checkIn || !checkOut) return null;
    const diff = new Date(checkOut) - new Date(checkIn);
    if (diff <= 0) return null;
    const totalMinutes = Math.floor(diff / 60000);
    const hours   = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    if (hours > 0) return `${hours}h ${minutes}min`;
    return `${minutes}min`;
}