function refreshQR() {
    const qrImage = document.getElementById('qrCodeImage');
    const refreshBtn = document.querySelector('.refresh-qr-btn');

    refreshBtn.disabled = true;
    refreshBtn.innerHTML = '<i class="bi bi-arrow-clockwise spinner-border spinner-border-sm"></i> Actualizando...';

    fetch('/api/qr/refresh', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
        .then(response => {
            if (response.ok) {
                qrImage.src = '/api/qr/image?t=' + new Date().getTime();
                showAlert('Código QR actualizado correctamente', 'success');
            } else {
                showAlert('Error al actualizar el código QR', 'danger');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('Error de conexión al actualizar el código QR', 'danger');
        })
        .finally(() => {
            refreshBtn.disabled = false;
            refreshBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Actualizar Código';
        });
}

function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
    document.body.appendChild(alertDiv);

    setTimeout(() => {
        alertDiv.remove();
    }, 3000);
}