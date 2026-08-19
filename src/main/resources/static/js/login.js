(function () {
    var form = document.getElementById("login-form");
    if (!form) {
        return;
    }

    function showError(name, message) {
        var input = form.querySelector("[name='" + name + "']");
        var error = form.querySelector("[data-error-for='" + name + "']");
        if (input) {
            input.classList.add("is-invalid");
        }
        if (error) {
            error.hidden = false;
            error.textContent = message;
        }
    }

    function clearErrors() {
        form.querySelectorAll(".is-invalid").forEach(function (el) {
            el.classList.remove("is-invalid");
        });
        form.querySelectorAll("[data-error-for]").forEach(function (el) {
            el.hidden = true;
            el.textContent = "";
        });
    }

    form.addEventListener("submit", function (event) {
        clearErrors();
        var username = form.username.value.trim();
        var password = form.password.value;
        var valid = true;
        if (!username) {
            showError("username", "Username is required");
            valid = false;
        }
        if (!password) {
            showError("password", "Password is required");
            valid = false;
        }
        if (!valid) {
            event.preventDefault();
        }
    });
})();
