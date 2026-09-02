function printInvoice() {
    const element = document.getElementById('factura-pdf');

    const opt = {
        margin: [0.5, 0.5],
        filename: 'Factura_FitnessLife.pdf',
        image: { type: 'jpeg', quality: 1 },
        html2canvas: { scale: 3, useCORS: true, logging: true }, // scale 3 para nitidez
        jsPDF: { unit: 'in', format: 'letter', orientation: 'portrait' }
    };

    html2pdf().set(opt).from(element).save();
}
