(function () {
    var form = document.getElementById("register-form");
    if (!form) {
        return;
    }

    var usernamePattern = /^[A-Za-z0-9_]{3,32}$/;
    var emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    var phonePattern = /^[+]?[0-9\s().-]{7,20}$/;

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

    function clearClientErrors() {
        form.querySelectorAll("[data-error-for]").forEach(function (el) {
            el.hidden = true;
            el.textContent = "";
        });
        form.querySelectorAll(".is-invalid").forEach(function (el) {
            el.classList.remove("is-invalid");
        });
    }

    form.addEventListener("submit", function (event) {
        clearClientErrors();
        var username = form.username.value.trim();
        var email = form.email.value.trim();
        var fullName = form.fullName.value.trim();
        var phone = form.phone.value.trim();
        var password = form.password.value;
        var confirmPassword = form.confirmPassword.value;
        var valid = true;

        if (!username) {
            showError("username", "Username is required");
            valid = false;
        } else if (!usernamePattern.test(username)) {
            showError("username", "Use 3 to 32 letters, numbers, or underscores");
            valid = false;
        }

        if (!email) {
            showError("email", "Email is required");
            valid = false;
        } else if (!emailPattern.test(email)) {
            showError("email", "Enter a valid email address");
            valid = false;
        }

        if (!fullName || fullName.length < 2) {
            showError("fullName", "Full name is required");
            valid = false;
        }

        if (phone && !phonePattern.test(phone)) {
            showError("phone", "Enter a valid phone number");
            valid = false;
        }

        if (!password) {
            showError("password", "Password is required");
            valid = false;
        } else if (password.length < 8) {
            showError("password", "Password must be at least 8 characters");
            valid = false;
        }

        if (!confirmPassword) {
            showError("confirmPassword", "Confirm your password");
            valid = false;
        } else if (password !== confirmPassword) {
            showError("confirmPassword", "Passwords do not match");
            valid = false;
        }

        if (!valid) {
            event.preventDefault();
        }
    });
})();
