(function () {
    var form = document.getElementById("profile-form");
    if (!form) {
        return;
    }

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
        var email = form.email.value.trim();
        var fullName = form.fullName.value.trim();
        var phone = form.phone.value.trim();
        var currentPassword = form.currentPassword.value;
        var newPassword = form.newPassword.value;
        var confirmPassword = form.confirmPassword.value;
        var valid = true;

        if (!fullName || fullName.length < 2) {
            showError("fullName", "Full name is required");
            valid = false;
        }

        if (!email) {
            showError("email", "Email is required");
            valid = false;
        } else if (!emailPattern.test(email)) {
            showError("email", "Enter a valid email address");
            valid = false;
        }

        if (phone && !phonePattern.test(phone)) {
            showError("phone", "Enter a valid phone number");
            valid = false;
        }

        var changingPassword = currentPassword || newPassword || confirmPassword;
        if (changingPassword) {
            if (!currentPassword) {
                showError("currentPassword", "Enter your current password to change it");
                valid = false;
            }
            if (!newPassword || newPassword.length < 8) {
                showError("newPassword", "New password must be at least 8 characters");
                valid = false;
            }
            if (newPassword !== confirmPassword) {
                showError("confirmPassword", "Passwords do not match");
                valid = false;
            }
        }

        if (!valid) {
            event.preventDefault();
        }
    });
})();
