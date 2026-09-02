document.addEventListener('DOMContentLoaded', () => {
    const filterName       = document.getElementById('filterName');
    const filterInstructor = document.getElementById('filterInstructor');
    const btnClear         = document.getElementById('btnClearFilters');
    const btnNoResultsClear= document.getElementById('btnNoResultsClear');
    const noResults        = document.getElementById('classesNoResults');
    const realCards        = document.querySelectorAll('.carousel-item-wrapper.real-card');
    const placeholders     = document.querySelectorAll('.ph-card');

    const MIN_VISIBLE = 3;

    const instructors = new Set();
    realCards.forEach(card => {
        const name = card.dataset.instructor?.trim();
        if (name) instructors.add(name);
    });
    instructors.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        filterInstructor.appendChild(opt);
    });

    function applyFilters() {
        const nameVal       = filterName.value.toLowerCase().trim();
        const instructorVal = filterInstructor.value.trim();
        const hasFilter     = nameVal || instructorVal;

        let visible = 0;

        realCards.forEach(card => {
            const matchName       = !nameVal || card.dataset.name.toLowerCase().includes(nameVal);
            const matchInstructor = !instructorVal || card.dataset.instructor === instructorVal;

            if (matchName && matchInstructor) {
                card.style.display = '';
                visible++;
            } else {
                card.style.display = 'none';
            }
        });

        const needed = Math.max(0, MIN_VISIBLE - visible);
        placeholders.forEach((ph, i) => {
            ph.style.display = i < needed ? '' : 'none';
        });

        if (noResults) {
            noResults.classList.toggle('d-none', !(hasFilter && visible === 0));
        }

        btnClear.style.display = hasFilter ? '' : 'none';
    }

    filterName.addEventListener('input', applyFilters);
    filterInstructor.addEventListener('change', applyFilters);

    const clearAll = () => {
        filterName.value = '';
        filterInstructor.value = '';
        applyFilters();
    };

    btnClear.addEventListener('click', clearAll);
    if (btnNoResultsClear) btnNoResultsClear.addEventListener('click', clearAll);

    applyFilters();
});