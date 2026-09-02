document.addEventListener('DOMContentLoaded', function () {

    const createForm = document.querySelector('#createPlanModal form');
    if (createForm) {
        createForm.addEventListener('submit', function (e) {
            const benefitsTextarea = this.querySelector('[name="benefitsText"]');
            const benefitsText = benefitsTextarea.value;

            const benefitsArray = benefitsText
                .split('\n')
                .map(b => b.trim())
                .filter(b => b.length > 0);

            this.querySelectorAll('[name^="benefits["]').forEach(el => el.remove());

            benefitsArray.forEach((benefit, index) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = `benefits[${index}]`;
                input.value = benefit;
                this.appendChild(input);
            });
        });
    }

    const editForm = document.querySelector('#editPlanForm');
    if (editForm) {
        editForm.addEventListener('submit', function (e) {
            const benefitsTextarea = this.querySelector('#editBenefitsText');
            const benefitsText = benefitsTextarea.value;

            const benefitsArray = benefitsText
                .split('\n')
                .map(b => b.trim())
                .filter(b => b.length > 0);

            this.querySelectorAll('[name^="benefits["]').forEach(el => el.remove());

            benefitsArray.forEach((benefit, index) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = `benefits[${index}]`;
                input.value = benefit;
                this.appendChild(input);
            });
        });
    }

    document.querySelectorAll(".btnEditar").forEach((button) => {
        button.addEventListener("click", async function () {
            const planId = this.getAttribute('data-plan-id');

            try {
                const response = await fetch(`/admin/plans/get/${planId}`);
                if (!response.ok) {
                    throw new Error('Error al obtener el plan');
                }

                const plan = await response.json();

                document.getElementById('editPlanId').value = plan.id;
                document.getElementById('editPlanName').value = plan.planName;
                document.getElementById('editPrice').value = plan.price;
                document.getElementById('editCurrency').value = plan.currency;
                document.getElementById('editDurationDays').value = plan.durationDays;
                document.getElementById('editBadge').value = plan.badge;

                const benefitsText = plan.benefits ? plan.benefits.join('\n') : '';
                document.getElementById('editBenefitsText').value = benefitsText;

                const editFormElement = document.getElementById('editPlanForm');
                editFormElement.action = `/admin/plans/update/${plan.id}`;

                const modal = new bootstrap.Modal(document.getElementById('editPlanModal'));
                modal.show();

            } catch (error) {
                console.error('Error:', error);
                alert('Error al cargar los datos del plan');
            }
        });
    });

    document.querySelectorAll('.alert').forEach(alert => {
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

function showBenefitsModal(button) {
    const planId = button.getAttribute('data-plan-id');

    fetch(`/admin/plans/get/${planId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al obtener el plan');
            }
            return response.json();
        })
        .then(plan => {
            const benefitsList = document.getElementById('benefitsList');
            benefitsList.innerHTML = '';

            if (plan.benefits && plan.benefits.length > 0) {
                plan.benefits.forEach(benefit => {
                    const li = document.createElement('li');
                    li.className = 'list-group-item bg-transparent text-white border-0';
                    li.innerHTML = `${benefit}`;
                    benefitsList.appendChild(li);
                });
            } else {
                benefitsList.innerHTML = '<li class="list-group-item text-muted">No hay beneficios registrados</li>';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('benefitsList').innerHTML =
                '<li class="list-group-item text-danger">Error al cargar beneficios</li>';
        });
}