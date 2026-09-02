const inputs = document.querySelectorAll(".input-field");
const toggleBtn = document.querySelectorAll(".toggle");
const main = document.querySelector("main");
const bullets = document.querySelectorAll(".bullets span");
const images = document.querySelectorAll(".image");

inputs.forEach((inp) => {
    inp.addEventListener("focus", () => {
        inp.classList.add("active");
    });
    inp.addEventListener("blur", () => {
        if (inp.value != "") return;
        inp.classList.remove("active");
    });
});

toggleBtn.forEach((btn) => {
    btn.addEventListener("click", () => {
        main.classList.toggle("sign-up-mode");
    });
});

function moveSlider() {
    let index = this.dataset.value;

    let currentImage = document.querySelector(`.img-${index}`);
    images.forEach(img => img.classList.remove("show"));
    currentImage.classList.add("show");

    bullets.forEach((bull) => bull.classList.remove("active"));
    this.classList.add("active");
}

bullets.forEach((bullet) => {
    bullet.addEventListener("click", moveSlider);
});

let currentSlide = 1;
const totalSlides = 3;

function autoSlide() {
    currentSlide++;
    if (currentSlide > totalSlides) {
        currentSlide = 1;
    }

    const currentBullet = document.querySelector(`.bullets span[data-value="${currentSlide}"]`);
    if (currentBullet) {
        moveSlider.call(currentBullet);
    }
}

const carouselInterval = setInterval(autoSlide, 3000);

bullets.forEach((bullet) => {
    bullet.addEventListener("click", () => {
        clearInterval(carouselInterval);
        setTimeout(() => {
            currentSlide = parseInt(bullet.dataset.value);
            setInterval(autoSlide, 3000);
        }, 5000);
    });
});