(function () {
    var dialog = document.getElementById("rating-dialog");
    var form = document.getElementById("rating-dialog-form");
    var dismiss = document.getElementById("rating-dialog-dismiss");
    var error = document.getElementById("rating-stars-error");
    if (!dialog || !form) {
        return;
    }

    function selectedStars() {
        return form.querySelector("input[name='stars']:checked");
    }

    document.querySelectorAll(".js-open-rating").forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            var action = button.getAttribute("data-action");
            if (!action) {
                return;
            }
            form.setAttribute("action", action);
            form.querySelectorAll("input[name='stars']").forEach(function (input) {
                input.checked = false;
            });
            if (form.review) {
                form.review.value = "";
            }
            if (error) {
                error.hidden = true;
            }
            if (typeof dialog.showModal === "function") {
                dialog.showModal();
            } else {
                dialog.setAttribute("open", "open");
            }
        });
    });

    if (dismiss) {
        dismiss.addEventListener("click", function () {
            if (typeof dialog.close === "function") {
                dialog.close();
            } else {
                dialog.removeAttribute("open");
            }
        });
    }

    form.addEventListener("submit", function (event) {
        if (!selectedStars()) {
            event.preventDefault();
            if (error) {
                error.hidden = false;
                error.textContent = "Choose a rating from 1 to 5 stars";
            }
        }
    });
})();
