function toggleDetails(button) {
    const paymentItem = button.closest('.payment-item');
    const details = paymentItem.querySelector('.payment-details');
    const icon = button.querySelector('i');

    details.classList.toggle('expanded');

    if (details.classList.contains('expanded')) {
        icon.classList.remove('bi-chevron-down');
        icon.classList.add('bi-chevron-up');
        button.innerHTML = '<i class="bi bi-chevron-up"></i> Ver menos detalles';
    } else {
        icon.classList.remove('bi-chevron-up');
        icon.classList.add('bi-chevron-down');
        button.innerHTML = '<i class="bi bi-chevron-down"></i> Ver más detalles';
    }
}