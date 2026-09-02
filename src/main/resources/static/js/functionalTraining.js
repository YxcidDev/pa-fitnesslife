
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.training-form').forEach(form => {

        const editBtn = form.querySelector('.btn-action-edit');
        const saveBtn = form.querySelector('.btn-save');
        const cancelBtn = form.querySelector('.btn-action-cancel');

        const viewFields = form.querySelectorAll('.view-mode');
        const editFields = form.querySelectorAll('.edit-mode');

        const originalValues = {};
        editFields.forEach(input => {
            originalValues[input.name] = input.value;
        });

        editBtn.addEventListener('click', () => {
            viewFields.forEach(el => el.style.display = 'none');
            editFields.forEach(el => el.style.display = 'block');
            editBtn.style.display = 'none';
        });

        cancelBtn.addEventListener('click', () => {
            editFields.forEach(input => {
                input.value = originalValues[input.name];
                input.style.display = 'none';
            });
            viewFields.forEach(el => el.style.display = 'block');
            editBtn.style.display = 'inline-block';
        });
    });
});

function mostrarFormulario() {
    document.getElementById("Formulario").classList.remove("d-none");
}

function ocultarFormulario() {
    document.getElementById("Formulario").classList.add("d-none");
}
