document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.btnEditStatus').forEach(button => {
        button.addEventListener('click', function () {
            const paymentId = this.getAttribute('data-payment-id');
            const currentPage = this.getAttribute('data-current-page') || '0';
            const statusFilter = this.getAttribute('data-status-filter') || '';
            const searchTerm = this.getAttribute('data-search-term') || '';

            document.getElementById('currentPageInput').value = currentPage;
            document.getElementById('statusFilterInput').value = statusFilter;
            document.getElementById('searchTermInput').value = searchTerm;

            const form = document.getElementById('editStatusForm');
            form.action = `/admin/payments/update-status/${paymentId}`;

            const modal = new bootstrap.Modal(document.getElementById('editStatusModal'));
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

function viewPaymentDetails(button) {
    const paymentId = button.getAttribute('data-payment-id');
    const modalContent = document.getElementById('paymentDetailsContent');

    modalContent.innerHTML = `
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Cargando...</span>
            </div>
        </div>
    `;

    fetch(`/admin/payments/get/${paymentId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al obtener el pago');
            }
            return response.json();
        })
        .then(payment => {
            const statusBadge = getStatusBadge(payment.status);
            const isActive = payment.active ? '<span class="badge bg-success ms-2"><i class="fas fa-check-circle"></i> Activo</span>' : '';

            modalContent.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Información del usuario</h6>
                        <div class="mb-2">
                            <strong>Nombre:</strong> ${payment.user.name} ${payment.user.lastname}
                        </div>
                        <div class="mb-2">
                            <strong>Email:</strong> ${payment.user.email}
                        </div>
                        <div class="mb-2">
                            <strong>Identificación:</strong> ${payment.user.identification}
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Información del plan</h6>
                        <div class="mb-2">
                            <strong>Plan:</strong> ${payment.plan.planName}
                        </div>
                        <div class="mb-2">
                            <strong>Duración:</strong> ${payment.plan.durationDays} días
                        </div>
                        <div class="mb-2">
                            <strong>Badge:</strong> <span class="plan-badge">${payment.plan.badge}</span>
                        </div>
                    </div>
                </div>
                
                <hr class="my-4"/>
                
                <div class="row">
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Información de pago</h6>
                        <div class="mb-2">
                            <strong>Monto:</strong> 
                            <span>${payment.currency} ${formatNumber(payment.amount)}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Estado:</strong> ${statusBadge} ${isActive}
                        </div>
                        <div class="mb-2">
                            <strong>Factura:</strong> ${payment.externalInvoice || 'N/A'}
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h6 class="text-muted mb-3">Detalles de transacción</h6>
                        <div class="mb-2">
                            <strong>ID Transacción:</strong> ${payment.transactionId || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Código Aprobación:</strong> ${payment.approvalCode || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Banco:</strong> ${payment.bankName || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Franquicia:</strong> ${payment.franchise || 'N/A'}
                        </div>
                    </div>
                </div>
                
                ${payment.validFrom ? `
                <hr class="my-4"/>
                <div class="row">
                    <div class="col-12">
                        <h6 class="text-muted mb-3">Validez del plan</h6>
                        <div class="mb-2">
                            <strong>Válido desde:</strong> ${formatDate(payment.validFrom)}
                        </div>
                        <div class="mb-2">
                            <strong>Válido hasta:</strong> ${formatDate(payment.validUntil)}
                        </div>
                    </div>
                </div>
                ` : ''}
                
                ${payment.responseReason ? `
                <hr class="my-4"/>
                <div class="row">
                    <div class="col-12">
                        <h6 class="text-muted mb-3">Respuesta del procesador</h6>
                        <div class="mb-2">
                            <strong>Código:</strong> ${payment.responseCode || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Respuesta:</strong> ${payment.responseText || 'N/A'}
                        </div>
                        <div class="mb-2">
                            <strong>Razón:</strong> ${payment.responseReason || 'N/A'}
                        </div>
                    </div>
                </div>
                ` : ''}
                
                <hr class="my-4"/>
                <div class="row">
                    <div class="col-md-6">
                        <small class="text-muted">
                            <strong>Creado:</strong> ${formatDate(payment.createdAt)}
                        </small>
                    </div>
                    ${payment.transactionDate ? `
                    <div class="col-md-6">
                        <small class="text-muted">
                            <strong>Fecha transacción:</strong> ${formatDate(payment.transactionDate)}
                        </small>
                    </div>
                    ` : ''}
                </div>
            `;
        })
        .catch(error => {
            console.error('Error:', error);
            modalContent.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-circle me-2"></i>
                    Error al cargar los detalles del pago
                </div>
            `;
        });
}

function getStatusBadge(status) {
    const badges = {
        'ACCEPTED': '<span class="badge bg-success">Aceptado</span>',
        'PENDING': '<span class="badge bg-warning text-dark">Pendiente</span>',
        'REJECTED': '<span class="badge bg-danger">Rechazado</span>',
        'FAILED': '<span class="badge bg-secondary">Fallido</span>'
    };
    return badges[status] || '<span class="badge bg-secondary">Desconocido</span>';
}

function formatNumber(number) {
    return new Intl.NumberFormat('es-CO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(number);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('es-CO', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}