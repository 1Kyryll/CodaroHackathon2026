(function () {
    document.querySelectorAll(".alert").forEach(function (alert) {
        window.setTimeout(function () {
            alert.style.opacity = "0";
            alert.style.transition = "opacity 400ms ease";
        }, 6000);
    });

    var reveals = document.querySelectorAll(".reveal");
    if (!reveals.length) {
        return;
    }
    var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduce || !("IntersectionObserver" in window)) {
        reveals.forEach(function (el) {
            el.classList.add("is-visible");
        });
        return;
    }
    var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add("is-visible");
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.14, rootMargin: "0px 0px -48px 0px" });
    reveals.forEach(function (el) {
        observer.observe(el);
    });
})();
