const editBtn = document.getElementById('editBtn');
const saveBtn = document.getElementById('saveBtn');
const cancelBtn = document.getElementById('cancelBtn');

editBtn.addEventListener('click', () => {
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'block');

    editBtn.style.display = 'none';
    saveBtn.style.display = 'inline-block';
    cancelBtn.style.display = 'inline-block';
});

cancelBtn.addEventListener('click', () => {
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'inline-block');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'none');

    editBtn.style.display = 'inline-block';
    saveBtn.style.display = 'none';
    cancelBtn.style.display = 'none';

    const form = editBtn.closest('form');
    if (form) form.reset();
});