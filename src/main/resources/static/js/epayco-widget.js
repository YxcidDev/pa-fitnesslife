document.addEventListener('DOMContentLoaded', function () {
    const payButtons = document.querySelectorAll('.plan-select-btn');

    payButtons.forEach(button => {
        button.addEventListener('click', function () {
            const planName = this.dataset.name;
            const description = this.dataset.description;
            const invoice = this.dataset.invoice;
            const currency = this.dataset.currency;
            const amount = this.dataset.amount;
            const planId = this.dataset.planid;
            const userId = this.dataset.userid;

            const externalInvoice = `INV-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

            createPendingPayment(userId, planId, externalInvoice)
                .then(() => {
                    openEpaycoCheckout({
                        name: planName,
                        description: description,
                        invoice: externalInvoice,
                        currency: currency,
                        amount: amount,
                        planId: planId,
                        userId: userId
                    });
                })
                .catch(error => {
                    console.error('Error creando pago:', error);
                    alert('Error al iniciar el proceso de pago. Por favor intenta nuevamente.');
                });
        });
    });
});

async function createPendingPayment(userId, planId, externalInvoice) {
    const response = await fetch('/api/payment/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            userId: userId,
            planId: planId,
            externalInvoice: externalInvoice
        })
    });

    if (!response.ok) {
        throw new Error('Error creando pago pendiente');
    }

    return response.json();
}

function openEpaycoCheckout(data) {
    const baseUrl = window.location.origin;
    const handler = ePayco.checkout.configure({
        key: 'a6cab5b1342bbbb93e223a9ba5b3519d',
        test: true
    });

    const checkoutData = {
        name: data.name,
        description: data.description,
        invoice: data.invoice,
        currency: data.currency,
        amount: data.amount,
        tax_base: '0',
        tax: '0',
        country: 'co',
        lang: 'es',

        external: 'false',
        response: `${baseUrl}/payment/response`,
        confirmation: `${baseUrl}/payment/confirmation`,

        extra1: data.planId,
        extra2: data.userId,
        extra3: 'fitness-life',

        methodsDisable: []
    };

    console.log('Abriendo checkout de ePayco con datos:', checkoutData);

    handler.open(checkoutData);
}