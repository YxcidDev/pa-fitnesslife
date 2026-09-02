console.log("UserTable.js cargado");
document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll(".btnEditar").forEach((button) => {
        button.addEventListener("click", async function () {
            const userId = this.getAttribute('data-user-id');

            try {
                const response = await fetch(`/admin/users/get/${userId}`);
                if (!response.ok) {
                    throw new Error('Error al obtener el usuario');
                }

                const user = await response.json();

                document.getElementById('editName').value = user.name || '';
                document.getElementById('editLastname').value = user.lastname || '';
                document.getElementById('editEmail').value = user.email || '';
                document.getElementById('editIdentification').value = user.identification || '';
                document.getElementById('editPhone').value = user.phone || '';
                document.getElementById('editSex').value = user.sex || '';
                document.getElementById('editBloodType').value = user.bloodType || '';
                document.getElementById('editRole').value = user.role || 'USER';
                document.getElementById('editPlan').value = user.plan || '';
                document.getElementById('editActive').value = user.active ? 'true' : 'false';

                if (user.birthDate) {
                    document.getElementById('editBirthDate').value = user.birthDate;
                }

                const editForm = document.getElementById('editUserForm');
                editForm.action = `/admin/users/update/${user.id}`;

                const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
                modal.show();

            } catch (error) {
                console.error('Error:', error);
                alert('Error al cargar los datos del usuario');
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